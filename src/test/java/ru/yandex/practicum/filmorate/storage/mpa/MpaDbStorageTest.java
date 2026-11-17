package ru.yandex.practicum.filmorate.storage.mpa;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import ru.yandex.practicum.filmorate.mapper.MpaRowMapper;
import ru.yandex.practicum.filmorate.model.Mpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@JdbcTest
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.ANY
)
@ActiveProfiles("test")
@Import({MpaDbStorage.class, MpaRowMapper.class})
class MpaDbStorageTest {

    private final MpaDbStorage mpaStorage;

    @Autowired
    MpaDbStorageTest(MpaDbStorage mpaStorage) {
        this.mpaStorage = mpaStorage;
    }

    @Test
    void findAll_shouldReturnFiveRatingsOrderedById() {
        assertThat(mpaStorage.findAll())
                .extracting(Mpa::getId, Mpa::getName)
                .containsExactly(
                        tuple(1L, "G"),
                        tuple(2L, "PG"),
                        tuple(3L, "PG-13"),
                        tuple(4L, "R"),
                        tuple(5L, "NC-17")
                );
    }

    @Test
    void findById_shouldReturnExistingRating() {
        assertThat(mpaStorage.findById(3L))
                .hasValueSatisfying(mpa -> {
                    assertThat(mpa.getId()).isEqualTo(3L);
                    assertThat(mpa.getName()).isEqualTo("PG-13");
                });
    }

    @Test
    void findById_shouldReturnEmpty_whenRatingDoesNotExist() {
        assertThat(mpaStorage.findById(500L)).isEmpty();
    }
}