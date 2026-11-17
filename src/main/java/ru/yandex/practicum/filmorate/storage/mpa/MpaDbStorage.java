package ru.yandex.practicum.filmorate.storage.mpa;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.mapper.MpaRowMapper;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.util.List;
import java.util.Optional;

@Repository
public class MpaDbStorage {

    private final JdbcTemplate jdbc;
    private final MpaRowMapper mapper;

    public MpaDbStorage(JdbcTemplate jdbc, MpaRowMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    public List<Mpa> findAll() {
        String sql = """
                SELECT id, name
                FROM mpa
                ORDER BY id
                """;

        return jdbc.query(sql, mapper);
    }

    public Optional<Mpa> findById(Long id) {
        String sql = """
                SELECT id, name
                FROM mpa
                WHERE id = ?
                """;

        try {
            Mpa mpa = jdbc.queryForObject(sql, mapper, id);
            return Optional.ofNullable(mpa);
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }
}