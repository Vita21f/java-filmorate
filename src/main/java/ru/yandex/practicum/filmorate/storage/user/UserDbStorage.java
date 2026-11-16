package ru.yandex.practicum.filmorate.storage.user;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.mapper.UserRowMapper;
import ru.yandex.practicum.filmorate.model.FriendshipStatus;
import ru.yandex.practicum.filmorate.model.User;

import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.*;

@Repository
public class UserDbStorage implements UserStorage {
    private final JdbcTemplate jdbc;
    private final UserRowMapper mapper;

    public UserDbStorage(JdbcTemplate jdbc, UserRowMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    @Override
    public Collection<User> findAll() {
        String sql = """
            SELECT id, email, login, name, birthday
            FROM users
            ORDER BY id
            """;

        List<User> users = jdbc.query(sql, mapper);
        loadFriends(users);

        return users;
    }

    @Override
    public Optional<User> findById(Long id) {
        String sql = """
            SELECT id, email, login, name, birthday
            FROM users
            WHERE id = ?
            """;

        User user;

        try {
            user = jdbc.queryForObject(sql, mapper, id);
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }

        if (user == null) {
            return Optional.empty();
        }

        loadFriends(List.of(user));

        return Optional.of(user);
    }

    @Override
    public User create(User user) {
        String sql = """
                INSERT INTO users (email, login, name, birthday)
                VALUES (?, ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbc.update(connection -> {
            PreparedStatement statement =
                    connection.prepareStatement(sql, new String[]{"id"});

            statement.setString(1, user.getEmail());
            statement.setString(2, user.getLogin());
            statement.setString(3, user.getName());
            statement.setObject(4, user.getBirthday(), Types.DATE);

            return statement;
        }, keyHolder);

        Number generatedId = keyHolder.getKey();

        if (generatedId == null) {
            throw new IllegalStateException(
                    "База данных не вернула ID созданного пользователя"
            );
        }

        user.setId(generatedId.longValue());
        return user;
    }

    @Override
    public User update(User user) {
        String sql = """
                UPDATE users
                SET email = ?, login = ?, name = ?, birthday = ?
                WHERE id = ?
                """;

        int updatedRows = jdbc.update(
                sql,
                user.getEmail(),
                user.getLogin(),
                user.getName(),
                user.getBirthday(),
                user.getId()
        );

        if (updatedRows == 0) {
            throw new NotFoundException(
                    "Пользователь с id = " + user.getId() + " не найден"
            );
        }

        return user;
    }

    @Override
    public void delete(Long id) {
        String sql = "DELETE FROM users WHERE id = ?";
        jdbc.update(sql, id);
    }

    @Override
    @Transactional
    public void addFriend(Long userId, Long friendId) {
        String reverseQuery = """
            SELECT COUNT(*)
            FROM friendship
            WHERE user_id = ? AND friend_id = ?
            """;

        Integer reverseCount = jdbc.queryForObject(
                reverseQuery,
                Integer.class,
                friendId,
                userId
        );

        boolean hasReverseFriendship =
                reverseCount != null && reverseCount > 0;

        FriendshipStatus status = hasReverseFriendship
                ? FriendshipStatus.CONFIRMED
                : FriendshipStatus.PENDING;

        String mergeQuery = """
            MERGE INTO friendship (user_id, friend_id, status)
            KEY (user_id, friend_id)
            VALUES (?, ?, ?)
            """;

        jdbc.update(mergeQuery, userId, friendId, status.name());

        if (hasReverseFriendship) {
            String confirmQuery = """
                UPDATE friendship
                SET status = ?
                WHERE user_id = ? AND friend_id = ?
                """;

            jdbc.update(
                    confirmQuery,
                    FriendshipStatus.CONFIRMED.name(),
                    friendId,
                    userId
            );
        }
    }

    @Override
    @Transactional
    public void removeFriend(Long userId, Long friendId) {
        String deleteQuery = """
            DELETE FROM friendship
            WHERE user_id = ? AND friend_id = ?
            """;

        jdbc.update(deleteQuery, userId, friendId);

        String updateReverseQuery = """
            UPDATE friendship
            SET status = ?
            WHERE user_id = ? AND friend_id = ?
            """;

        jdbc.update(
                updateReverseQuery,
                FriendshipStatus.PENDING.name(),
                friendId,
                userId
        );
    }

    private void loadFriends(Collection<User> users) {
        if (users.isEmpty()) {
            return;
        }

        Map<Long, User> usersById = new HashMap<>();

        for (User user : users) {
            user.getFriends().clear();
            usersById.put(user.getId(), user);
        }

        String placeholders = String.join(
                ", ",
                Collections.nCopies(usersById.size(), "?")
        );

        String sql = """
            SELECT user_id, friend_id, status
            FROM friendship
            WHERE user_id IN (%s)
            ORDER BY user_id, friend_id
            """.formatted(placeholders);

        jdbc.query(sql, (RowCallbackHandler) rs -> {
            Long userId = rs.getLong("user_id");
            Long friendId = rs.getLong("friend_id");

            FriendshipStatus status =
                    FriendshipStatus.valueOf(rs.getString("status"));

            usersById.get(userId).getFriends().put(friendId, status);
        }, usersById.keySet().toArray());
    }
}
