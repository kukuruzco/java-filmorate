package ru.yandex.practicum.filmorate.db;

import jakarta.validation.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

@Repository
@Qualifier("userDbStorage")
@Primary
@Sql(scripts = {"/schema.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class UserDbStorage implements UserStorage {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public UserDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<User> getAll() {
        String sql = "SELECT * FROM users ORDER BY id";
        List<User> users = jdbcTemplate.query(sql, this::mapRowToUser);
        loadFriendsForUsers(users);
        return users;
    }

    @Override
    public User get(Long id) {
        try {
            String sql = "SELECT * FROM users WHERE id = ?";
            User user = jdbcTemplate.queryForObject(sql, this::mapRowToUser, id);
            user.setFriends(getFriendIds(id));
            return user;
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public User create(User user) {
        SimpleJdbcInsert simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("users")
                .usingGeneratedKeyColumns("id");

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("email", user.getEmail());
        parameters.put("login", user.getLogin());
        parameters.put("name", user.getName());
        parameters.put("birthday", user.getBirthday());

        Number generatedId = simpleJdbcInsert.executeAndReturnKey(parameters);
        user.setId(generatedId.longValue());
        return user;
    }

    @Override
    @Transactional
    public User update(User user) {
        String sql = "UPDATE users SET email = ?, login = ?, name = ?, birthday = ? WHERE id = ?";

        int rowsUpdated = jdbcTemplate.update(sql,
                user.getEmail(),
                user.getLogin(),
                user.getName(),
                user.getBirthday(),
                user.getId());

        return rowsUpdated > 0 ? get(user.getId()) : null;
    }

    @Override
    public boolean delete(Long id) {
        String sql = "DELETE FROM users WHERE id = ?";
        return jdbcTemplate.update(sql, id) > 0;
    }

    @Override
    public boolean exists(Long id) {
        String sql = "SELECT COUNT(*) FROM users WHERE id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }

    private User mapRowToUser(ResultSet rs, int rowNum) throws SQLException {
        return User.builder()
                .id(rs.getLong("id"))
                .email(rs.getString("email"))
                .login(rs.getString("login"))
                .name(rs.getString("name"))
                .birthday(rs.getObject("birthday", LocalDate.class))
                .build();
    }

    private Set<Long> getFriendIds(Long userId) {
        String sql = "SELECT friend_id FROM friendships WHERE user_id = ?";
        return new HashSet<>(jdbcTemplate.queryForList(sql, Long.class, userId));
    }

    private void loadFriendsForUsers(List<User> users) {
        if (users.isEmpty()) return;

        List<Long> userIds = users.stream().map(User::getId).toList();
        String inClause = String.join(",", Collections.nCopies(userIds.size(), "?"));

        String sql = String.format(
                "SELECT user_id, friend_id FROM friendships WHERE status = 'confirmed' AND user_id IN (%s) " +
                        "UNION ALL " +
                        "SELECT friend_id as user_id, user_id as friend_id FROM friendships WHERE status = 'confirmed' AND friend_id IN (%s)",
                inClause, inClause
        );

        Object[] params = new Object[userIds.size() * 2];
        for (int i = 0; i < userIds.size(); i++) {
            params[i] = userIds.get(i);
            params[i + userIds.size()] = userIds.get(i);
        }

        Map<Long, Set<Long>> userFriendsMap = jdbcTemplate.query(sql, params, rs -> {
            Map<Long, Set<Long>> result = new HashMap<>();
            while (rs.next()) {
                Long userId = rs.getLong("user_id");
                Long friendId = rs.getLong("friend_id");
                result.computeIfAbsent(userId, k -> new HashSet<>()).add(friendId);
            }
            return result;
        });

        users.forEach(user ->
                user.setFriends(userFriendsMap.getOrDefault(user.getId(), new HashSet<>()))
        );
    }

    public void addFriend(Long userId, Long friendId) {
        String checkSql = "SELECT COUNT(*) FROM friendships WHERE user_id = ? AND friend_id = ?";
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, userId, friendId);

        if (count > 0) {
            throw new ValidationException("Пользователи уже друзья");
        }

        String insertSql = "INSERT INTO friendships (user_id, friend_id, status) VALUES (?, ?, 'unconfirmed')";

        try {
            jdbcTemplate.update(insertSql, userId, friendId);
        } catch (DataIntegrityViolationException e) {
            throw new NotFoundException("Один из пользователей не найден");
        }
    }

    public void removeFriend(Long userId, Long friendId) {
        String sql = "DELETE FROM friendships WHERE user_id = ? AND friend_id = ?";
        jdbcTemplate.update(sql, userId, friendId);
    }

    public List<User> getFriendsList(Long userId) {
        String sql = "SELECT u.* FROM users u " +
                "JOIN friendships f ON u.id = f.friend_id " +
                "WHERE f.user_id = ? " +
                "ORDER BY u.id";

        List<User> friends = jdbcTemplate.query(sql, this::mapRowToUser, userId);
        loadFriendsForUsers(friends);
        return friends;
    }

    public List<User> getCommonFriends(Long userId1, Long userId2) {
        Set<Long> friends1 = getFriendIds(userId1);
        Set<Long> friends2 = getFriendIds(userId2);

        Set<Long> commonIds = new HashSet<>(friends1);
        commonIds.retainAll(friends2);

        if (commonIds.isEmpty()) return Collections.emptyList();

        String inClause = String.join(",", Collections.nCopies(commonIds.size(), "?"));
        String sql = String.format("SELECT * FROM users WHERE id IN (%s) ORDER BY id", inClause);

        List<User> commonFriends = jdbcTemplate.query(sql, commonIds.toArray(), this::mapRowToUser);
        loadFriendsForUsers(commonFriends);
        return commonFriends;
    }
}