package ru.yandex.practicum.filmorate.storage.genre;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.mapper.GenreRowMapper;
import ru.yandex.practicum.filmorate.model.Genre;

import java.util.List;
import java.util.Optional;

@Repository
public class GenreDbStorage {

    private final JdbcTemplate jdbc;
    private final GenreRowMapper mapper;

    public GenreDbStorage(JdbcTemplate jdbc, GenreRowMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    public List<Genre> findAll() {
        String sql = """
                SELECT id, name
                FROM genres
                ORDER BY id
                """;

        return jdbc.query(sql, mapper);
    }

    public Optional<Genre> findById(Long id) {
        String sql = """
                SELECT id, name
                FROM genres
                WHERE id = ?
                """;

        try {
            Genre genre = jdbc.queryForObject(sql, mapper, id);
            return Optional.ofNullable(genre);
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }
}