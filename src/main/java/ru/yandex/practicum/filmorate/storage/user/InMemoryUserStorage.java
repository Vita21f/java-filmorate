package ru.yandex.practicum.filmorate.storage.user;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.FriendshipStatus;
import ru.yandex.practicum.filmorate.model.User;

import java.util.*;

@Component
public class InMemoryUserStorage implements UserStorage {
    private final Map<Long, User> users = new HashMap<>();
    private long nextId = 0;

    @Override
    public Collection<User> findAll() {
        return List.copyOf(users.values());
    }

    @Override
    public User create(User user) {
        user.setId(getNextId());
        users.put(user.getId(), user);
        return user;
    }

    @Override
    public User update(User user) {
        users.put(user.getId(), user);
        return user;
    }

    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public void delete(Long id) {
        for (User user : users.values()) {
            user.getFriends().remove(id);
        }

        users.remove(id);
    }

    @Override
    public void addFriend(Long userId, Long friendId) {
        User user = findById(userId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Пользователь с id = " + userId + " не найден"
                        )
                );

        User friend = findById(friendId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Пользователь с id = " + friendId + " не найден"
                        )
                );

        boolean hasReverseFriendship =
                friend.getFriends().containsKey(userId);

        FriendshipStatus status = hasReverseFriendship
                ? FriendshipStatus.CONFIRMED
                : FriendshipStatus.PENDING;

        user.getFriends().put(friendId, status);

        if (hasReverseFriendship) {
            friend.getFriends().put(userId, FriendshipStatus.CONFIRMED);
        }
    }

    @Override
    public void removeFriend(Long userId, Long friendId) {
        User user = findById(userId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Пользователь с id = " + userId + " не найден"
                        )
                );

        User friend = findById(friendId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Пользователь с id = " + friendId + " не найден"
                        )
                );

        user.getFriends().remove(friendId);

        if (friend.getFriends().containsKey(userId)) {
            friend.getFriends().put(userId, FriendshipStatus.PENDING);
        }
    }

    private long getNextId() {
        return ++nextId;
    }
}
