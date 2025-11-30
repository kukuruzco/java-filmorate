package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.User;

import java.util.Collection;
import java.util.List;

public interface UserStorage {
    List<User> getAll();

    User get(Long id);

    User create(User user);

    User update(User user);

    boolean delete(Long id);

    boolean exists(Long id);
}
