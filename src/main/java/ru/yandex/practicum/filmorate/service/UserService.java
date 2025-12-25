package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserStorage;
import ru.yandex.practicum.filmorate.storage.db.UserDbStorage;

import java.util.List;

@Slf4j
@Service
public class UserService {
    private final UserStorage userStorage;
    private final UserDbStorage userDbStorage;

    @Autowired
    public UserService(UserStorage userStorage, UserDbStorage userDbStorage) {
        this.userStorage = userStorage;
        this.userDbStorage = userDbStorage;
    }

    public List<User> getAll() {
        log.info("Получен запрос на получение всех пользователей");
        return userStorage.getAll();
    }

    public User getById(Long id) {
        User user = userStorage.get(id);
        if (user == null) {
            throw new NotFoundException("Пользователь с id = " + id + " не найден");
        }
        return user;
    }

    public User create(User user) {
        log.info("Получен запрос на создание пользователя: {}", user);

        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
            log.debug("Имя не указано, установлено равным логину: {}", user.getLogin());
        }

        return userStorage.create(user);
    }

    public User update(User user) {
        log.info("Получен запрос на обновление пользователя: {}", user);

        isValidUser(user);

        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
            log.debug("Имя не указано, установлено равным логину: {}", user.getLogin());
        }

        return userStorage.update(user);
    }

    public void addFriend(Long userId, Long friendId) {
        log.info("Получен запрос на добавление в друзья: пользователь {} добавляет пользователя {}", userId, friendId);

        getUserOrThrow(userId);
        getUserOrThrow(friendId);

        if (userId.equals(friendId)) {
            throw new IllegalArgumentException("Нельзя добавить самого себя в друзья");
        }

        userDbStorage.addFriend(userId, friendId);
    }

    public void removeFriend(Long userId, Long friendId) {
        log.info("Получен запрос на удаление из друзей: пользователь {} удаляет пользователя {}", userId, friendId);

        getUserOrThrow(userId);
        getUserOrThrow(friendId);

        userDbStorage.removeFriend(userId, friendId);
    }

    public List<User> getFriends(Long userId) {
        log.info("Получен запрос на получение друзей пользователя {}", userId);
        getUserOrThrow(userId);
        return userDbStorage.getFriendsList(userId);
    }

    public List<User> getCommonFriends(Long userId1, Long userId2) {
        log.info("Получен запрос на получение общих друзей пользователей {} и {}", userId1, userId2);
        getUserOrThrow(userId1);
        getUserOrThrow(userId2);
        return userDbStorage.getCommonFriends(userId1, userId2);
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

    private User getUserOrThrow(Long userId) {
        User user = userStorage.get(userId);
        if (user == null) {
            throw new NotFoundException("Пользователь с id = " + userId + " не найден");
        }
        return user;
    }
}