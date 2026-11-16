package ru.yandex.practicum.filmorate.service;

import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.storage.mpa.MpaDbStorage;

import java.util.List;

@Service
public class MpaService {

    private final MpaDbStorage mpaStorage;

    public MpaService(MpaDbStorage mpaStorage) {
        this.mpaStorage = mpaStorage;
    }

    public List<Mpa> findAll() {
        return mpaStorage.findAll();
    }

    public Mpa getById(Long id) {
        return mpaStorage.findById(id)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Рейтинг с id = " + id + " не найден"
                        )
                );
    }
}