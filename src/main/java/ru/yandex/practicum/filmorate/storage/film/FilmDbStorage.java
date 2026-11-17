package ru.yandex.practicum.filmorate.storage.film;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.mapper.FilmRowMapper;
import ru.yandex.practicum.filmorate.mapper.GenreRowMapper;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;

import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.*;

@Repository
public class FilmDbStorage implements FilmStorage {

    private static final String SELECT_FILMS = """
            SELECT
                f.id,
                f.name,
                f.description,
                f.release_date,
                f.duration,
                m.id AS mpa_id,
                m.name AS mpa_name
            FROM films AS f
            JOIN mpa AS m ON f.mpa_id = m.id
            """;

    private final JdbcTemplate jdbc;
    private final FilmRowMapper mapper;
    private final GenreRowMapper genreMapper;

    public FilmDbStorage(
            JdbcTemplate jdbc,
            FilmRowMapper mapper,
            GenreRowMapper genreMapper
    ) {
        this.jdbc = jdbc;
        this.mapper = mapper;
        this.genreMapper = genreMapper;
    }

    @Override
    public Collection<Film> findAll() {
        String sql = SELECT_FILMS + " ORDER BY f.id";

        List<Film> films = jdbc.query(sql, mapper);
        loadRelations(films);

        return films;
    }

    @Override
    public Optional<Film> findById(Long id) {
        String sql = SELECT_FILMS + " WHERE f.id = ?";

        Film film;

        try {
            film = jdbc.queryForObject(sql, mapper, id);
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }

        if (film == null) {
            return Optional.empty();
        }

        loadRelations(List.of(film));

        return Optional.of(film);
    }

    @Override
    @Transactional
    public Film create(Film film) {
        String sql = """
            INSERT INTO films (
                name,
                description,
                release_date,
                duration,
                mpa_id
            )
            VALUES (?, ?, ?, ?, ?)
            """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbc.update(connection -> {
            PreparedStatement statement =
                    connection.prepareStatement(sql, new String[]{"id"});

            statement.setString(1, film.getName());
            statement.setString(2, film.getDescription());
            statement.setObject(3, film.getReleaseDate(), Types.DATE);
            statement.setInt(4, film.getDuration());
            statement.setLong(5, film.getMpa().getId());

            return statement;
        }, keyHolder);

        Number generatedId = keyHolder.getKey();

        if (generatedId == null) {
            throw new IllegalStateException(
                    "База данных не вернула ID созданного фильма"
            );
        }

        Long filmId = generatedId.longValue();

        saveGenres(filmId, film.getGenres());

        film.setId(filmId);

        return findById(filmId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Не удалось прочитать созданный фильм с id = " + filmId
                        )
                );
    }

    @Override
    @Transactional
    public Film update(Film film) {
        String sql = """
            UPDATE films
            SET name = ?,
                description = ?,
                release_date = ?,
                duration = ?,
                mpa_id = ?
            WHERE id = ?
            """;

        int updatedRows = jdbc.update(
                sql,
                film.getName(),
                film.getDescription(),
                film.getReleaseDate(),
                film.getDuration(),
                film.getMpa().getId(),
                film.getId()
        );

        if (updatedRows == 0) {
            throw new NotFoundException(
                    "Фильм с id = " + film.getId() + " не найден"
            );
        }

        jdbc.update(
                "DELETE FROM film_genres WHERE film_id = ?",
                film.getId()
        );

        saveGenres(film.getId(), film.getGenres());

        return findById(film.getId())
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Не удалось прочитать обновлённый фильм с id = "
                                        + film.getId()
                        )
                );
    }

    @Override
    public void delete(Long id) {
        String sql = "DELETE FROM films WHERE id = ?";
        jdbc.update(sql, id);
    }

    @Override
    public void addLike(Long filmId, Long userId) {
        String sql = """
            MERGE INTO likes (film_id, user_id)
            KEY (film_id, user_id)
            VALUES (?, ?)
            """;

        jdbc.update(sql, filmId, userId);
    }

    @Override
    public void removeLike(Long filmId, Long userId) {
        String sql = """
            DELETE FROM likes
            WHERE film_id = ? AND user_id = ?
            """;

        jdbc.update(sql, filmId, userId);
    }

    @Override
    public Collection<Film> findPopular(int count) {
        String sql = SELECT_FILMS + """
             LEFT JOIN (
                 SELECT film_id, COUNT(*) AS likes_count
                 FROM likes
                 GROUP BY film_id
             ) AS popularity ON popularity.film_id = f.id
             ORDER BY COALESCE(popularity.likes_count, 0) DESC, f.id ASC
             LIMIT ?
            """;

        List<Film> films = jdbc.query(sql, mapper, count);
        loadRelations(films);

        return films;
    }

    private void saveGenres(Long filmId, Collection<Genre> genres) {
        String sql = """
            INSERT INTO film_genres (film_id, genre_id)
            VALUES (?, ?)
            """;

        for (Genre genre : genres) {
            jdbc.update(sql, filmId, genre.getId());
        }
    }

    private void loadRelations(Collection<Film> films) {
        if (films.isEmpty()) {
            return;
        }

        Map<Long, Film> filmsById = new HashMap<>();

        for (Film film : films) {
            film.getGenres().clear();
            film.getLikes().clear();
            filmsById.put(film.getId(), film);
        }

        String placeholders = String.join(
                ", ",
                Collections.nCopies(filmsById.size(), "?")
        );

        Object[] filmIds = filmsById.keySet().toArray();

        String genresSql = """
            SELECT fg.film_id, g.id, g.name
            FROM film_genres AS fg
            JOIN genres AS g ON fg.genre_id = g.id
            WHERE fg.film_id IN (%s)
            ORDER BY fg.film_id, g.id
            """.formatted(placeholders);

        jdbc.query(genresSql, (RowCallbackHandler) rs -> {
            Long filmId = rs.getLong("film_id");
            Genre genre = genreMapper.mapRow(rs, 0);

            filmsById.get(filmId).getGenres().add(genre);
        }, filmIds);

        String likesSql = """
            SELECT film_id, user_id
            FROM likes
            WHERE film_id IN (%s)
            """.formatted(placeholders);

        jdbc.query(likesSql, (RowCallbackHandler) rs -> {
            Long filmId = rs.getLong("film_id");
            Long userId = rs.getLong("user_id");

            filmsById.get(filmId).getLikes().add(userId);
        }, filmIds);
    }
}