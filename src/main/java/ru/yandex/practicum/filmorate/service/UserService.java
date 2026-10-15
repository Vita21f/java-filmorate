package ru.yandex.practicum.filmorate.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.model.FriendshipStatus;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.Collection;
import java.util.Set;

@Service
public class UserService {
    private final UserStorage userStorage;

    @Autowired
    public UserService(UserStorage userStorage) {
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
            throw new ValidationException("Нельзя добавить самого себя в друзья");
        }

        User user = getById(userId);
        User friendUser = getById(friendId);

        user.getFriends().put(friendId, FriendshipStatus.PENDING);
        friendUser.getFriends().put(userId, FriendshipStatus.PENDING);
    }

    public void removeFriend(Long userId, Long friendId) {
        User user = getById(userId);
        User friendUser = getById(friendId);

        user.getFriends().remove(friendId);
        friendUser.getFriends().remove(userId);
    }

    public Collection<User> getFriends(Long userId) {
        return getById(userId).getFriends().keySet()
                .stream()
                .map(this::getById)
                .toList();
    }

    public Collection<User> getCommonFriends(Long userId, Long otherId) {
        Set<Long> otherFriends = getById(otherId).getFriends().keySet();

        return getById(userId).getFriends().keySet().stream()
                .filter(otherFriends::contains)
                .map(this::getById)
                .toList();
    }

    public void delete(Long id) {
        User user = getById(id);

        for (Long friendId : user.getFriends().keySet()) {
            User friend = getById(friendId);
            friend.getFriends().remove(id);
        }

        userStorage.delete(id);
    }

    private String getDisplayName(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            return user.getLogin();
        }
        return user.getName();
    }
}
