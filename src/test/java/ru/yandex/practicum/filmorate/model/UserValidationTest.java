package ru.yandex.practicum.filmorate.model;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class UserValidationTest {
    private static ValidatorFactory factory;
    private Validator validator;

    @BeforeAll
    static void beforeAll() {
        factory = Validation.buildDefaultValidatorFactory();
    }

    @BeforeEach
    void beforeEach() {
        validator = factory.getValidator();
    }

    @AfterAll
    static void afterAll() {
        factory.close();
    }

    @Test
    void shouldHaveViolation_whenEmailIsNull() {
        User user = validUser();
        user.setEmail(null);

        Set<ConstraintViolation<User>> violations = validator.validate(user);

        assertFalse(violations.isEmpty());
    }

    @Test
    void shouldHaveViolation_whenEmailIsBlank() {
        User user = validUser();
        user.setEmail(" ");

        Set<ConstraintViolation<User>> violations = validator.validate(user);

        assertFalse(violations.isEmpty());
    }

    @Test
    void shouldHaveViolation_whenEmailWithoutAt() {
        User user = validUser();
        user.setEmail("pypypy.com");

        Set<ConstraintViolation<User>> violations = validator.validate(user);

        assertFalse(violations.isEmpty());
    }

    @Test
    void shouldHaveViolation_whenLoginIsNull() {
        User user = validUser();
        user.setLogin(null);

        Set<ConstraintViolation<User>> violations = validator.validate(user);

        assertFalse(violations.isEmpty());
    }

    @Test
    void shouldHaveViolation_whenLoginIsBlank() {
        User user = validUser();
        user.setLogin(" ");

        Set<ConstraintViolation<User>> violations = validator.validate(user);

        assertFalse(violations.isEmpty());
    }

    @Test
    void shouldHaveViolation_whenLoginContainsSpaces() {
        User user = validUser();
        user.setLogin("My login");

        Set<ConstraintViolation<User>> violations = validator.validate(user);

        assertFalse(violations.isEmpty());
    }

    @Test
    void shouldHaveViolation_whenBirthdayIsAfterNow() {
        User user = validUser();
        user.setBirthday(LocalDate.now().plusDays(1));

        Set<ConstraintViolation<User>> violations = validator.validate(user);

        assertFalse(violations.isEmpty());
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
