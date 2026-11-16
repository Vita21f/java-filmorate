package ru.yandex.practicum.filmorate.storage.film;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.mapper.FilmRowMapper;
import ru.yandex.practicum.filmorate.mapper.GenreRowMapper;
import ru.yandex.practicum.filmorate.mapper.UserRowMapper;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@JdbcTest
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.ANY
)
@ActiveProfiles("test")
@Import({
        FilmDbStorage.class,
        FilmRowMapper.class,
        GenreRowMapper.class,
        UserDbStorage.class,
        UserRowMapper.class
})
class FilmDbStorageTest {

    private final FilmDbStorage filmStorage;
    private final UserDbStorage userStorage;
    private final JdbcTemplate jdbc;

    @Autowired
    FilmDbStorageTest(
            FilmDbStorage filmStorage,
            UserDbStorage userStorage,
            JdbcTemplate jdbc
    ) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
        this.jdbc = jdbc;
    }

    @Test
    void create_shouldSaveFilmWithRatingAndSortedGenres() {
        Film film = newFilm("Интерстеллар", 3L, 4L, 2L);

        Film created = filmStorage.create(film);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getId()).isPositive();

        assertThat(created.getMpa().getName()).isEqualTo("PG-13");
        assertThat(created.getGenres())
                .extracting(Genre::getId, Genre::getName)
                .containsExactly(
                        tuple(2L, "Драма"),
                        tuple(4L, "Триллер")
                );

        Film saved = filmStorage.findById(created.getId())
                .orElseThrow();

        assertThat(saved.getId()).isEqualTo(created.getId());
        assertThat(saved.getName()).isEqualTo("Интерстеллар");
        assertThat(saved.getDescription()).isEqualTo("Описание фильма");
        assertThat(saved.getReleaseDate())
                .isEqualTo(LocalDate.of(2014, 11, 6));
        assertThat(saved.getDuration()).isEqualTo(169);

        assertThat(saved.getMpa().getId()).isEqualTo(3L);
        assertThat(saved.getMpa().getName()).isEqualTo("PG-13");

        assertThat(saved.getGenres())
                .extracting(Genre::getId, Genre::getName)
                .containsExactly(
                        tuple(2L, "Драма"),
                        tuple(4L, "Триллер")
                );

        assertThat(saved.getLikes()).isEmpty();
    }

    @Test
    void create_shouldSaveFilmWithoutGenresAndDescription() {
        Film film = newFilm("Фильм без жанров", 1L);
        film.setDescription(null);

        Film created = filmStorage.create(film);

        Film saved = filmStorage.findById(created.getId())
                .orElseThrow();

        assertThat(saved.getDescription()).isNull();
        assertThat(saved.getGenres()).isEmpty();
        assertThat(saved.getMpa().getId()).isEqualTo(1L);
        assertThat(saved.getMpa().getName()).isEqualTo("G");
    }

    @Test
    void findById_shouldReturnEmpty_whenFilmDoesNotExist() {
        assertThat(filmStorage.findById(-1L)).isEmpty();
    }

    @Test
    void findAll_shouldReturnFilmsWithTheirOwnGenres() {
        Film first = filmStorage.create(
                newFilm("Первый фильм", 1L, 1L, 2L)
        );

        Film second = filmStorage.create(
                newFilm("Второй фильм", 4L, 4L)
        );

        List<Film> films = List.copyOf(filmStorage.findAll());

        assertThat(films)
                .extracting(Film::getId)
                .containsExactly(first.getId(), second.getId());

        assertThat(films.get(0).getMpa().getName()).isEqualTo("G");
        assertThat(films.get(0).getGenres())
                .extracting(Genre::getId)
                .containsExactly(1L, 2L);

        assertThat(films.get(1).getMpa().getName()).isEqualTo("R");
        assertThat(films.get(1).getGenres())
                .extracting(Genre::getId)
                .containsExactly(4L);
    }

    @Test
    void findAll_shouldReturnEmpty_whenNoFilmsExist() {
        assertThat(filmStorage.findAll()).isEmpty();
    }

    private Film newFilm(String name, Long mpaId, Long... genreIds) {
        Film film = new Film();

        film.setName(name);
        film.setDescription("Описание фильма");
        film.setReleaseDate(LocalDate.of(2014, 11, 6));
        film.setDuration(169);

        Mpa mpa = new Mpa();
        mpa.setId(mpaId);
        film.setMpa(mpa);

        for (Long genreId : genreIds) {
            Genre genre = new Genre();
            genre.setId(genreId);

            film.getGenres().add(genre);
        }

        return film;
    }

    @Test
    void update_shouldReplaceFieldsRatingAndGenres() {
        Film created = filmStorage.create(
                newFilm("Исходное название", 3L, 1L, 2L)
        );

        Film changes = newFilm("Новое название", 4L, 4L, 2L);
        changes.setId(created.getId());
        changes.setDescription("Новое описание");
        changes.setReleaseDate(LocalDate.of(2020, 2, 15));
        changes.setDuration(120);

        Film updated = filmStorage.update(changes);

        assertThat(updated.getId()).isEqualTo(created.getId());
        assertThat(updated.getMpa().getName()).isEqualTo("R");
        assertThat(updated.getGenres())
                .extracting(Genre::getId, Genre::getName)
                .containsExactly(
                        tuple(2L, "Драма"),
                        tuple(4L, "Триллер")
                );

        Film saved = filmStorage.findById(created.getId()).orElseThrow();

        assertThat(saved.getName()).isEqualTo("Новое название");
        assertThat(saved.getDescription()).isEqualTo("Новое описание");
        assertThat(saved.getReleaseDate())
                .isEqualTo(LocalDate.of(2020, 2, 15));
        assertThat(saved.getDuration()).isEqualTo(120);
        assertThat(saved.getMpa().getId()).isEqualTo(4L);
        assertThat(saved.getMpa().getName()).isEqualTo("R");
        assertThat(saved.getGenres())
                .extracting(Genre::getId, Genre::getName)
                .containsExactly(
                        tuple(2L, "Драма"),
                        tuple(4L, "Триллер")
                );
    }

    @Test
    void update_shouldRemoveAllGenres_whenNewSetIsEmpty() {
        Film created = filmStorage.create(
                newFilm("Фильм", 3L, 1L, 2L)
        );

        Film changes = newFilm("Фильм", 3L);
        changes.setId(created.getId());

        filmStorage.update(changes);

        Film saved = filmStorage.findById(created.getId()).orElseThrow();

        assertThat(saved.getGenres()).isEmpty();
        assertThat(saved.getMpa().getId()).isEqualTo(3L);
    }

    @Test
    void update_shouldThrow_whenFilmDoesNotExist() {
        Film film = newFilm("Несуществующий фильм", 1L);
        film.setId(-1L);

        assertThatThrownBy(() -> filmStorage.update(film))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void delete_shouldRemoveFilmWithoutAffectingAnotherFilm() {
        Film first = filmStorage.create(
                newFilm("Первый фильм", 1L, 1L, 2L)
        );

        Film second = filmStorage.create(
                newFilm("Второй фильм", 3L, 2L)
        );

        filmStorage.delete(first.getId());

        assertThat(filmStorage.findById(first.getId())).isEmpty();

        assertThat(filmStorage.findAll())
                .extracting(Film::getId)
                .containsExactly(second.getId());

        Film savedSecond = filmStorage.findById(second.getId())
                .orElseThrow();

        assertThat(savedSecond.getGenres())
                .extracting(Genre::getId, Genre::getName)
                .containsExactly(tuple(2L, "Драма"));
    }

    @Test
    void addLike_shouldSaveLikeWithoutDuplicates() {
        Film film = filmStorage.create(newFilm("Фильм", 1L));
        User user = createUser("anna");

        filmStorage.addLike(film.getId(), user.getId());
        filmStorage.addLike(film.getId(), user.getId());

        Film saved = filmStorage.findById(film.getId()).orElseThrow();

        assertThat(saved.getLikes()).containsExactly(user.getId());

        Integer count = jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM likes
                WHERE film_id = ? AND user_id = ?
                """,
                Integer.class,
                film.getId(),
                user.getId()
        );

        assertThat(count).isEqualTo(1);
    }

    @Test
    void removeLike_shouldRemoveOnlySelectedUsersLike() {
        Film film = filmStorage.create(newFilm("Фильм", 1L));
        User anna = createUser("anna");
        User boris = createUser("boris");

        filmStorage.addLike(film.getId(), anna.getId());
        filmStorage.addLike(film.getId(), boris.getId());

        filmStorage.removeLike(film.getId(), anna.getId());
        filmStorage.removeLike(film.getId(), anna.getId());

        Film saved = filmStorage.findById(film.getId()).orElseThrow();

        assertThat(saved.getLikes()).containsExactly(boris.getId());
    }

    @Test
    void update_shouldPreserveLikes() {
        Film film = filmStorage.create(newFilm("Старое название", 1L, 1L));
        User user = createUser("anna");

        filmStorage.addLike(film.getId(), user.getId());

        Film changes = newFilm("Новое название", 3L, 2L);
        changes.setId(film.getId());

        filmStorage.update(changes);

        Film saved = filmStorage.findById(film.getId()).orElseThrow();

        assertThat(saved.getName()).isEqualTo("Новое название");
        assertThat(saved.getGenres())
                .extracting(Genre::getId)
                .containsExactly(2L);
        assertThat(saved.getLikes()).containsExactly(user.getId());
    }

    @Test
    void delete_shouldRemoveFilmGenresAndLikes() {
        Film film = filmStorage.create(newFilm("Фильм", 1L, 1L, 2L));
        User user = createUser("anna");

        filmStorage.addLike(film.getId(), user.getId());

        filmStorage.delete(film.getId());

        assertThat(filmStorage.findById(film.getId())).isEmpty();

        Integer likesCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM likes WHERE film_id = ?",
                Integer.class,
                film.getId()
        );

        Integer genresCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM film_genres WHERE film_id = ?",
                Integer.class,
                film.getId()
        );

        assertThat(likesCount).isZero();
        assertThat(genresCount).isZero();
        assertThat(userStorage.findById(user.getId())).isPresent();
    }

    @Test
    void findAll_shouldLoadLikesForCorrectFilms() {
        Film first = filmStorage.create(newFilm("Первый", 1L));
        Film second = filmStorage.create(newFilm("Второй", 1L));
        User anna = createUser("anna");
        User boris = createUser("boris");

        filmStorage.addLike(first.getId(), anna.getId());
        filmStorage.addLike(second.getId(), boris.getId());

        List<Film> films = List.copyOf(filmStorage.findAll());

        assertThat(films)
                .extracting(Film::getId)
                .containsExactly(first.getId(), second.getId());

        assertThat(films.get(0).getLikes()).containsExactly(anna.getId());
        assertThat(films.get(1).getLikes()).containsExactly(boris.getId());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void create_shouldRollbackFilmAndGenres_whenGenreDoesNotExist() {
        Film film = newFilm("Фильм с ошибкой", 1L, 2L, 500L);

        assertThatThrownBy(() -> filmStorage.create(film))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(filmStorage.findAll()).isEmpty();

        Integer genresCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM film_genres",
                Integer.class
        );

        assertThat(genresCount).isZero();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void update_shouldRestoreOriginalData_whenGenreDoesNotExist() {
        Film created = filmStorage.create(
                newFilm("Исходное название", 3L, 1L, 2L)
        );

        Film changes = newFilm("Новое название", 4L, 4L, 500L);
        changes.setId(created.getId());
        changes.setDescription("Изменённое описание");
        changes.setDuration(120);

        assertThatThrownBy(() -> filmStorage.update(changes))
                .isInstanceOf(DataIntegrityViolationException.class);

        Film saved = filmStorage.findById(created.getId()).orElseThrow();

        assertThat(saved.getName()).isEqualTo("Исходное название");
        assertThat(saved.getDescription()).isEqualTo("Описание фильма");
        assertThat(saved.getDuration()).isEqualTo(169);
        assertThat(saved.getMpa().getId()).isEqualTo(3L);

        assertThat(saved.getGenres())
                .extracting(Genre::getId, Genre::getName)
                .containsExactly(
                        tuple(1L, "Комедия"),
                        tuple(2L, "Драма")
                );
    }

    private User createUser(String login) {
        User user = new User();

        user.setEmail(login + "@example.com");
        user.setLogin(login);
        user.setName(login);
        user.setBirthday(LocalDate.of(2000, 1, 1));

        return userStorage.create(user);
    }
}