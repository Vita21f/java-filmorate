package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserControllerTest {
    private UserController controller;

    @BeforeEach
    void beforeEach() {
        controller = new UserController();
    }

    @Test
    void create_shouldCreateUser_whenDataIsValid() {
        User user = validUser();
        controller.create(user);

        assertEquals(1, controller.findAll().size());
    }

    @Test
    void create_shouldCreateUser_whenBirthdayIsNull() {
        User user = validUser();
        user.setBirthday(null);
        controller.create(user);

        assertEquals(1, controller.findAll().size());
    }

    @Test
    void create_shouldCreateUser_whenBirthdayIsNow() {
        User user = validUser();
        user.setBirthday(LocalDate.now());
        controller.create(user);

        assertEquals(1, controller.findAll().size());
    }

    @Test
    void create_shouldUseLoginAsName_whenNameIsNull() {
        User user = validUser();
        user.setName(null);

        User created = controller.create(user);

        assertEquals("pam", created.getName());
        assertEquals(1, controller.findAll().size());
    }

    @Test
    void update_shouldThrow_whenNewUserIdIsNull() {
        controller.create(validUser());

        User newUser = validUser();
        newUser.setId(null);

        assertThrows(ValidationException.class, () -> controller.update(newUser));
    }

    @Test
    void update_shouldUseLoginAsName_whenNameIsNull() {
        User created = controller.create(validUser());

        User newUser = validUser();
        newUser.setId(created.getId());
        newUser.setName(null);

        User updated = controller.update(newUser);

        assertEquals(1, controller.findAll().size());
        assertEquals("pam", updated.getName());
    }

    @Test
    void update_shouldUseLoginAsName_whenNameIsBlank() {
        User created = controller.create(validUser());

        User newUser = validUser();
        newUser.setId(created.getId());
        newUser.setName(" ");

        User updated = controller.update(newUser);

        assertEquals(1, controller.findAll().size());
        assertEquals("pam", updated.getName());
    }

    @Test
    void update_shouldThrow_whenIdIsNotFound() {
        controller.create(validUser());
        User user = validUser();
        user.setId(999L);

        assertThrows(NotFoundException.class, () -> controller.update(user));
    }

    @Test
    void update_shouldUpdateFields_whenDataIsValid() {
        User created = controller.create(validUser());

        User newUser = validUser();
        newUser.setId(created.getId());
        newUser.setEmail("new@gmail.com");
        newUser.setLogin("newLogin");

        User updated = controller.update(newUser);

        assertEquals("new@gmail.com", updated.getEmail());
        assertEquals("newLogin", updated.getLogin());
        assertEquals(created.getId(), updated.getId());
        assertEquals(1, controller.findAll().size());
    }

    private User validUser() {
        User user = new User();
        user.setEmail("pampam@gmail.com");
        user.setLogin("pam");
        user.setName("имя");
        user.setBirthday(LocalDate.of(2000, 1, 10));
        return user;
    }
}
