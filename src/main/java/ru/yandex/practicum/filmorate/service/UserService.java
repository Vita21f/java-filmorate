package ru.yandex.practicum.filmorate.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.Collection;

@Service
public class UserService {
    private final UserStorage userStorage;

    @Autowired
    public UserService(
            @Qualifier("userDbStorage") UserStorage userStorage) {
        this.userStorage = userStorage;
    }

    public Collection<User> findAll() {
        return userStorage.findAll();
    }

    public User getById(Long id) {
        return userStorage.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь с id = " + id + " не найден"));
    }

    public User create(User user) {
        user.setName(getDisplayName(user));
        return userStorage.create(user);
    }

    public User update(User newUser) {
        if (newUser.getId() == null) {
            throw new ValidationException("Поле id обязательно для обновления");
        }

        User oldUser = getById(newUser.getId());

        oldUser.setEmail(newUser.getEmail());
        oldUser.setLogin(newUser.getLogin());
        oldUser.setBirthday(newUser.getBirthday());
        oldUser.setName(getDisplayName(newUser));
        return userStorage.update(oldUser);
    }

    public void addFriend(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            throw new ValidationException(
                    "Нельзя добавить самого себя в друзья"
            );
        }

        getById(userId);
        getById(friendId);

        userStorage.addFriend(userId, friendId);
    }

    public void removeFriend(Long userId, Long friendId) {
        getById(userId);
        getById(friendId);

        userStorage.removeFriend(userId, friendId);
    }

    public Collection<User> getFriends(Long userId) {
        checkUserExists(userId);

        return userStorage.findFriends(userId);
    }

    public Collection<User> getCommonFriends(Long userId, Long otherId) {
        checkUserExists(userId);
        checkUserExists(otherId);

        return userStorage.findCommonFriends(userId, otherId);
    }

    private void checkUserExists(Long id) {
        if (!userStorage.existsById(id)) {
            throw new NotFoundException(
                    "Пользователь с id = " + id + " не найден"
            );
        }
    }

    public void delete(Long id) {
        getById(id);
        userStorage.delete(id);
    }

    private String getDisplayName(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            return user.getLogin();
        }
        return user.getName();
    }
}
