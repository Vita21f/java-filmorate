package ru.yandex.practicum.filmorate.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;

import java.time.LocalDate;
import java.util.Collection;

@Service
public class FilmService {
    private static final LocalDate DATE_OF_FIRST_FILM = LocalDate.of(1895, 12, 28);
    private final FilmStorage filmStorage;
    private final UserService userService;
    private final MpaService mpaService;
    private final GenreService genreService;

    @Autowired
    public FilmService(
            @Qualifier("filmDbStorage") FilmStorage filmStorage,
            UserService userService,
            MpaService mpaService,
            GenreService genreService
    ) {
        this.filmStorage = filmStorage;
        this.userService = userService;
        this.mpaService = mpaService;
        this.genreService = genreService;
    }

    public Collection<Film> findAll() {
        return filmStorage.findAll();
    }

    public Film getById(Long id) {
        return filmStorage.findById(id)
                .orElseThrow(() -> new NotFoundException("Фильм с id = " + id + " не найден"));
    }

    public Film create(Film film) {
        validate(film);
        return filmStorage.create(film);
    }

    public Film update(Film newFilm) {
        if (newFilm.getId() == null) {
            throw new ValidationException("Поле id обязательно для обновления");
        }
        validate(newFilm);
        Film oldFilm = getById(newFilm.getId());

        oldFilm.setName(newFilm.getName());
        oldFilm.setDescription(newFilm.getDescription());
        oldFilm.setReleaseDate(newFilm.getReleaseDate());
        oldFilm.setDuration(newFilm.getDuration());
        oldFilm.setMpa(newFilm.getMpa());

        oldFilm.getGenres().clear();
        oldFilm.getGenres().addAll(newFilm.getGenres());

        return filmStorage.update(oldFilm);
    }

    public void addLike(Long filmId, Long userId) {
        getById(filmId);
        userService.getById(userId);

        filmStorage.addLike(filmId, userId);
    }

    public void removeLike(Long filmId, Long userId) {
        getById(filmId);
        userService.getById(userId);

        filmStorage.removeLike(filmId, userId);
    }

    public Collection<Film> getPopular(int count) {
        if (count < 0) {
            throw new ValidationException(
                    "Количество фильмов не может быть отрицательным"
            );
        }

        return filmStorage.findPopular(count);
    }

    public void delete(Long id) {
        getById(id);
        filmStorage.delete(id);
    }

    private void validate(Film film) {
        if (film.getReleaseDate() != null && film.getReleaseDate().isBefore(DATE_OF_FIRST_FILM)) {
            throw new ValidationException("Дата релиза должна быть не ранее выпуска первого фильма в истории");
        }

        if (film.getMpa() == null || film.getMpa().getId() == null) {
            throw new ValidationException(
                    "Необходимо указать идентификатор рейтинга"
            );
        }

        mpaService.getById(film.getMpa().getId());

        for (Genre genre : film.getGenres()) {
            if (genre == null || genre.getId() == null) {
                throw new ValidationException(
                        "Необходимо указать идентификатор жанра"
                );
            }

            genreService.getById(genre.getId());
        }
    }
}
