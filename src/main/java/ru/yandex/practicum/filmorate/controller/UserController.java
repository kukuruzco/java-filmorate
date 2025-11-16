package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/users")
public class UserController {
    private final Map<Long, User> users = new HashMap<>();

    @GetMapping
    public Collection<User> getUsers() {
        log.info("Получен запрос на получение всех пользователей");
        return users.values();
    }

    @PostMapping
    public User create(@Valid @RequestBody User newUser) {
        log.info("Получен запрос на создание пользователя: {}", newUser);

        if (newUser.getName() == null || newUser.getName().isBlank()) {
            newUser.setName(newUser.getLogin());
            log.debug("Имя не указано, установлено равным логину: {}", newUser.getLogin());
        }

        newUser.setId(getNextId());
        users.put(newUser.getId(), newUser);

        log.info("Пользователь создан с id: {}", newUser.getId());
        return newUser;
    }

    @PutMapping
    public User update(@Valid @RequestBody User updatedUser) {
        log.info("Получен запрос на обновление пользователя: {}", updatedUser);

        if (updatedUser.getId() == null) {
            log.warn("Id пользователя не указан");
            throw new ValidationException("Id должен быть указан");
        }

        if (!users.containsKey(updatedUser.getId())) {
            log.warn("Пользователь с id = {} не найден", updatedUser.getId());
            throw new NotFoundException("Пользователь с id = " + updatedUser.getId() + " не найден");
        }

        if (updatedUser.getName() == null || updatedUser.getName().isBlank()) {
            updatedUser.setName(updatedUser.getLogin());
            log.debug("Имя не указано, установлено равным логину: {}", updatedUser.getLogin());
        }

        users.put(updatedUser.getId(), updatedUser);

        log.info("Пользователь с id = {} обновлён", updatedUser.getId());
        return updatedUser;
    }

    private long getNextId() {
        long currentMaxId = users.keySet()
                .stream()
                .mapToLong(id -> id)
                .max()
                .orElse(0);
        return ++currentMaxId;
    }
}