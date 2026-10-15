package ru.yandex.practicum.filmorate.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;

@Service
public class FilmService {
    private static final LocalDate DATE_OF_FIRST_FILM = LocalDate.of(1895, 12, 28);
    private final FilmStorage filmStorage;
    private final UserService userService;

    @Autowired
    public FilmService(FilmStorage filmStorage, UserService userService) {
        this.filmStorage = filmStorage;
        this.userService = userService;
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

        return filmStorage.update(oldFilm);
    }

    public void addLike(Long filmId, Long userId) {
        Film film = getById(filmId);
        userService.getById(userId); // проверяем, что пользователь существует

        film.getLikes().add(userId);
    }

    public void removeLike(Long filmId, Long userId) {
        Film film = getById(filmId);
        userService.getById(userId); // проверяем, что пользователь существует

        film.getLikes().remove(userId);
    }

    public Collection<Film> getPopular(int count) {
        return filmStorage.findAll().stream()
                .sorted(Comparator.comparingInt((Film film) -> film.getLikes().size()).reversed())
                .limit(count)
                .toList();
    }

    public void delete(Long id) {
        getById(id);
        filmStorage.delete(id);
    }

    private void validate(Film film) {
        if (film.getReleaseDate() != null && film.getReleaseDate().isBefore(DATE_OF_FIRST_FILM)) {
            throw new ValidationException("Дата релиза должна быть не ранее выпуска первого фильма в истории");
        }
    }
}
