package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserStorage userStorage;
    private final Map<Long, Set<Long>> friends = new HashMap<>();

    public List<User> getAll() {
        log.info("Получен запрос на получение всех пользователей");
        return userStorage.getAll();
    }

    public User create(User user) {
        log.info("Получен запрос на создание пользователя: {}", user);

        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
            log.debug("Имя не указано, установлено равным логину: {}", user.getLogin());
        }

        User createdUser = userStorage.create(user);
        log.info("Пользователь создан с id: {}", createdUser.getId());
        return createdUser;
    }

    public User update(User user) {
        log.info("Получен запрос на обновление пользователя: {}", user);

        isValidUser(user);

        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
            log.debug("Имя не указано, установлено равным логину: {}", user.getLogin());
        }

        User updatedUser = userStorage.update(user);
        log.info("Пользователь с id = {} обновлён", updatedUser.getId());
        return updatedUser;
    }

    public User getById(Long id) {
        User user = userStorage.get(id);
        if (user == null) {
            throw new NotFoundException("Пользователь с id = " + id + " не найден");
        }
        return user;
    }

    public void addFriend(Long userId, Long friendId) {
        log.info("Получен запрос на добавление в друзья: пользователь {} добавляет пользователя {}", userId, friendId);

        User user = getUserOrThrow(userId);
        User friend = getUserOrThrow(friendId);

        if (userId.equals(friendId)) {
            log.warn("Пользователь {} пытается добавить самого себя в друзья", userId);
            throw new IllegalArgumentException("Нельзя добавить самого себя в друзья");
        }

        friends.putIfAbsent(userId, new HashSet<>());
        friends.putIfAbsent(friendId, new HashSet<>());

        Set<Long> userFriends = friends.get(userId);
        Set<Long> friendFriends = friends.get(friendId);

        if (userFriends.contains(friendId)) {
            log.warn("Пользователь {} уже в друзьях у пользователя {}", friendId, userId);
            throw new IllegalArgumentException("Пользователь уже в друзьях");
        }

        userFriends.add(friendId);
        friendFriends.add(userId);

        log.info("Пользователи {} и {} теперь друзья", userId, friendId);
    }

    public void removeFriend(Long userId, Long friendId) {
        log.info("Получен запрос на удаление из друзей: пользователь {} удаляет пользователя {}", userId, friendId);

        User user = getUserOrThrow(userId);
        User friend = getUserOrThrow(friendId);

        if (!friends.containsKey(userId) || !friends.get(userId).contains(friendId)) {
            log.warn("Пользователь {} не в друзьях у пользователя {}", friendId, userId);
            return;
        }

        friends.get(userId).remove(friendId);
        friends.get(friendId).remove(userId);

        log.info("Пользователи {} и {} больше не друзья", userId, friendId);
    }

    public List<User> getFriends(Long userId) {
        log.info("Получен запрос на получение друзей пользователя {}", userId);

        getUserOrThrow(userId);

        Set<Long> friendIds = friends.getOrDefault(userId, Collections.emptySet());
        return friendIds.stream()
                .map(this::getUserOrThrow)
                .collect(Collectors.toList());
    }

    public List<User> getCommonFriends(Long userId1, Long userId2) {
        log.info("Получен запрос на получение общих друзей пользователей {} и {}", userId1, userId2);

        getUserOrThrow(userId1);
        getUserOrThrow(userId2);

        Set<Long> friends1 = friends.getOrDefault(userId1, Collections.emptySet());
        Set<Long> friends2 = friends.getOrDefault(userId2, Collections.emptySet());

        Set<Long> commonFriendIds = new HashSet<>(friends1);
        commonFriendIds.retainAll(friends2);

        return commonFriendIds.stream()
                .map(this::getUserOrThrow)
                .collect(Collectors.toList());
    }

    private User getUserOrThrow(Long userId) {
        User user = userStorage.get(userId);
        if (user == null) {
            throw new NotFoundException("Пользователь с id = " + userId + " не найден");
        }
        return user;
    }

    private void isValidUser(User user) {
        if (user.getId() == null) {
            log.warn("Id пользователя не указан");
            throw new IllegalArgumentException("Id должен быть указан");
        }

        if (!userStorage.exists(user.getId())) {
            log.warn("Пользователь с id = {} не найден", user.getId());
            throw new NotFoundException("Пользователь с id = " + user.getId() + " не найден");
        }
    }
}