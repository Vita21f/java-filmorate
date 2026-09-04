package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FilmControllerTest {
    private FilmController controller;

    @BeforeEach
    void beforeEach() {
        controller = new FilmController();
    }

    @Test
    void create_shouldCreateFilm_whenDescriptionIs200Chars() {
        Film film = validFilm();

        film.setDescription("a".repeat(200));
        controller.create(film);

        assertEquals(1, controller.findAll().size());
    }

    @Test
    void create_shouldCreateFilm_whenDescriptionIsNull() {
        Film film = validFilm();

        film.setDescription(null);
        controller.create(film);

        assertEquals(1, controller.findAll().size());
    }

    @Test
    void create_shouldThrow_whenReleaseDateIsBeforeDateOfFirstFilm() {
        Film film = validFilm();

        film.setReleaseDate(LocalDate.of(1895, 12, 27));

        assertThrows(ValidationException.class, () -> controller.create(film));
    }

    @Test
    void create_shouldCreateFilm_whenReleaseDateIsDateOfFirstFilm() {
        Film film = validFilm();

        film.setReleaseDate(LocalDate.of(1895, 12, 28));
        controller.create(film);

        assertEquals(1, controller.findAll().size());
    }

    @Test
    void create_shouldCreateFilm_whenValidData() {
        Film film = validFilm();

        controller.create(film);

        assertEquals(1, controller.findAll().size());
    }

    @Test
    void update_shouldThrow_whenIdIsNull() {
        controller.create(validFilm());
        Film film = validFilm();

        assertThrows(ValidationException.class, () -> controller.update(film));
    }

    @Test
    void update_shouldThrow_whenIdIsNotFound() {
        controller.create(validFilm());
        Film film = validFilm();
        film.setId(999L);

        assertThrows(NotFoundException.class, () -> controller.update(film));
    }

    @Test
    void update_shouldUpdateFilm_whenNewFilmIsValid() {
        Film created = controller.create(validFilm());

        Film newFilm = validFilm();
        newFilm.setId(created.getId());
        newFilm.setName("другое название");

        Film updated = controller.update(newFilm);

        assertEquals("другое название", updated.getName());
        assertEquals(1, controller.findAll().size());
    }

    private Film validFilm() {
        Film film = new Film();

        film.setName("фильм");
        film.setDescription("описание");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(100);

        return film;
    }
}
