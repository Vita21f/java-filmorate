package ru.yandex.practicum.filmorate.storage.genre;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import ru.yandex.practicum.filmorate.mapper.GenreRowMapper;
import ru.yandex.practicum.filmorate.model.Genre;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@JdbcTest
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.ANY
)
@ActiveProfiles("test")
@Import({GenreDbStorage.class, GenreRowMapper.class})
class GenreDbStorageTest {

    private final GenreDbStorage genreStorage;

    @Autowired
    GenreDbStorageTest(GenreDbStorage genreStorage) {
        this.genreStorage = genreStorage;
    }

    @Test
    void findAll_shouldReturnSixGenresOrderedById() {
        assertThat(genreStorage.findAll())
                .extracting(Genre::getId, Genre::getName)
                .containsExactly(
                        tuple(1L, "Комедия"),
                        tuple(2L, "Драма"),
                        tuple(3L, "Мультфильм"),
                        tuple(4L, "Триллер"),
                        tuple(5L, "Документальный"),
                        tuple(6L, "Боевик")
                );
    }

    @Test
    void findById_shouldReturnExistingGenre() {
        assertThat(genreStorage.findById(2L))
                .hasValueSatisfying(genre -> {
                    assertThat(genre.getId()).isEqualTo(2L);
                    assertThat(genre.getName()).isEqualTo("Драма");
                });
    }

    @Test
    void findById_shouldReturnEmpty_whenGenreDoesNotExist() {
        assertThat(genreStorage.findById(500L)).isEmpty();
    }
}