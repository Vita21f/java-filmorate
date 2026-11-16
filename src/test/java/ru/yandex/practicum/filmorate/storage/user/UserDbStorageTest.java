package ru.yandex.practicum.filmorate.storage.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.mapper.UserRowMapper;
import ru.yandex.practicum.filmorate.model.FriendshipStatus;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@JdbcTest
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.ANY
)
@ActiveProfiles("test")
@Import({UserDbStorage.class, UserRowMapper.class})
class UserDbStorageTest {

    private final UserDbStorage userStorage;

    @Autowired
    UserDbStorageTest(UserDbStorage userStorage) {
        this.userStorage = userStorage;
    }

    @Test
    void create_shouldSaveUserAndGenerateId() {
        User user = newUser("anna");

        User created = userStorage.create(user);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getId()).isPositive();

        User saved = userStorage.findById(created.getId())
                .orElseThrow();

        assertThat(saved.getId()).isEqualTo(created.getId());
        assertThat(saved.getEmail()).isEqualTo("anna@example.com");
        assertThat(saved.getLogin()).isEqualTo("anna");
        assertThat(saved.getName()).isEqualTo("Имя anna");
        assertThat(saved.getBirthday())
                .isEqualTo(LocalDate.of(2000, 5, 10));
        assertThat(saved.getFriends()).isEmpty();
    }

    @Test
    void findById_shouldReturnEmpty_whenUserDoesNotExist() {
        Optional<User> result = userStorage.findById(-1L);

        assertThat(result).isEmpty();
    }

    @Test
    void findAll_shouldReturnSavedUsersOrderedById() {
        User anna = userStorage.create(newUser("anna"));
        User boris = userStorage.create(newUser("boris"));

        assertThat(userStorage.findAll())
                .extracting(User::getId)
                .containsExactly(anna.getId(), boris.getId());
    }

    @Test
    void update_shouldSaveChangedFields() {
        User user = userStorage.create(newUser("anna"));

        user.setEmail("updated@example.com");
        user.setLogin("updated");
        user.setName("Новое имя");
        user.setBirthday(LocalDate.of(1995, 1, 20));

        userStorage.update(user);

        User saved = userStorage.findById(user.getId()).orElseThrow();

        assertThat(saved.getId()).isEqualTo(user.getId());
        assertThat(saved.getEmail()).isEqualTo("updated@example.com");
        assertThat(saved.getLogin()).isEqualTo("updated");
        assertThat(saved.getName()).isEqualTo("Новое имя");
        assertThat(saved.getBirthday())
                .isEqualTo(LocalDate.of(1995, 1, 20));
    }

    @Test
    void update_shouldThrow_whenUserDoesNotExist() {
        User user = newUser("unknown");
        user.setId(-1L);

        assertThatThrownBy(() -> userStorage.update(user))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void create_shouldAllowNullBirthday() {
        User user = newUser("anna");
        user.setBirthday(null);

        User created = userStorage.create(user);

        User saved = userStorage.findById(created.getId()).orElseThrow();

        assertThat(saved.getBirthday()).isNull();
    }

    @Test
    void addFriend_shouldBeOneWayAndAllowRepeatedRequest() {
        User anna = userStorage.create(newUser("anna"));
        User boris = userStorage.create(newUser("boris"));

        userStorage.addFriend(anna.getId(), boris.getId());
        userStorage.addFriend(anna.getId(), boris.getId());

        User savedAnna = userStorage.findById(anna.getId()).orElseThrow();
        User savedBoris = userStorage.findById(boris.getId()).orElseThrow();

        assertThat(savedAnna.getFriends())
                .hasSize(1)
                .containsEntry(boris.getId(), FriendshipStatus.PENDING);

        assertThat(savedBoris.getFriends()).isEmpty();
    }

    @Test
    void addFriend_shouldConfirmBothDirections_whenRequestIsMutual() {
        User anna = userStorage.create(newUser("anna"));
        User boris = userStorage.create(newUser("boris"));

        userStorage.addFriend(anna.getId(), boris.getId());
        userStorage.addFriend(boris.getId(), anna.getId());

        User savedAnna = userStorage.findById(anna.getId()).orElseThrow();
        User savedBoris = userStorage.findById(boris.getId()).orElseThrow();

        assertThat(savedAnna.getFriends())
                .hasSize(1)
                .containsEntry(boris.getId(), FriendshipStatus.CONFIRMED);

        assertThat(savedBoris.getFriends())
                .hasSize(1)
                .containsEntry(anna.getId(), FriendshipStatus.CONFIRMED);
    }

    @Test
    void removeFriend_shouldKeepReverseDirectionAsPending() {
        User anna = userStorage.create(newUser("anna"));
        User boris = userStorage.create(newUser("boris"));

        userStorage.addFriend(anna.getId(), boris.getId());
        userStorage.addFriend(boris.getId(), anna.getId());

        userStorage.removeFriend(anna.getId(), boris.getId());
        userStorage.removeFriend(anna.getId(), boris.getId());

        User savedAnna = userStorage.findById(anna.getId()).orElseThrow();
        User savedBoris = userStorage.findById(boris.getId()).orElseThrow();

        assertThat(savedAnna.getFriends()).isEmpty();

        assertThat(savedBoris.getFriends())
                .hasSize(1)
                .containsEntry(anna.getId(), FriendshipStatus.PENDING);
    }

    @Test
    void delete_shouldRemoveUserAndFriendshipLinks() {
        User anna = userStorage.create(newUser("anna"));
        User boris = userStorage.create(newUser("boris"));

        userStorage.addFriend(anna.getId(), boris.getId());
        userStorage.addFriend(boris.getId(), anna.getId());

        userStorage.delete(anna.getId());

        assertThat(userStorage.findById(anna.getId())).isEmpty();

        User savedBoris = userStorage.findById(boris.getId()).orElseThrow();

        assertThat(savedBoris.getFriends()).isEmpty();

        assertThat(userStorage.findAll())
                .extracting(User::getId)
                .containsExactly(boris.getId());
    }

    private User newUser(String login) {
        User user = new User();

        user.setEmail(login + "@example.com");
        user.setLogin(login);
        user.setName("Имя " + login);
        user.setBirthday(LocalDate.of(2000, 5, 10));

        return user;
    }
}