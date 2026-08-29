package ru.yandex.practicum.filmorate.controller;

import java.util.Map;

public class ControllerUtils {
    public static long getNextId(Map<Long, ?> collection) {
        long currentMaxId = collection.keySet()
                .stream()
                .mapToLong(id -> id)
                .max()
                .orElse(0);
        return ++currentMaxId;
    }
}
