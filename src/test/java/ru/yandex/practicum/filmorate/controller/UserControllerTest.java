package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserControllerTest {

    private UserController userController;

    @BeforeEach
    void setUp() {
        userController = new UserController();
    }

    @Test
    void shouldSetNameToLoginWhenNameIsNull() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setLogin("validlogin");
        user.setBirthday(LocalDate.now().minusYears(1));
        user.setName(null);

        User result = userController.create(user);

        assertEquals("validlogin", result.getName());
    }

    @Test
    void shouldSetNameToLoginWhenNameIsBlank() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setLogin("validlogin");
        user.setBirthday(LocalDate.now().minusYears(1));
        user.setName("   ");

        User result = userController.create(user);

        assertEquals("validlogin", result.getName());
    }

    @Test
    void shouldGenerateIdWhenCreatingUser() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setLogin("validlogin");
        user.setBirthday(LocalDate.now().minusYears(1));

        User result = userController.create(user);

        assertNotNull(result.getId());
        assertEquals(1L, result.getId());
    }

    @Test
    void shouldIncrementIdForMultipleUsers() {
        User user1 = new User();
        user1.setEmail("test1@example.com");
        user1.setLogin("login1");
        user1.setBirthday(LocalDate.now().minusYears(1));

        User user2 = new User();
        user2.setEmail("test2@example.com");
        user2.setLogin("login2");
        user2.setBirthday(LocalDate.now().minusYears(2));

        User result1 = userController.create(user1);
        User result2 = userController.create(user2);

        assertEquals(1L, result1.getId());
        assertEquals(2L, result2.getId());
    }

    @Test
    void shouldThrowValidationExceptionWhenIdIsNullInUpdate() {
        User user = new User();
        user.setId(null);

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> userController.update(user)
        );
        assertTrue(exception.getMessage().contains("Id должен быть указан"));
    }

    @Test
    void shouldThrowNotFoundExceptionWhenUserDoesNotExistInUpdate() {
        User user = new User();
        user.setId(999L);
        user.setEmail("test@example.com");
        user.setLogin("validlogin");
        user.setBirthday(LocalDate.now().minusYears(1));

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> userController.update(user)
        );
        assertTrue(exception.getMessage().contains("не найден"));
    }

    @Test
    void shouldReplaceUserEntirely() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setLogin("validlogin");
        user.setName("Original Name");
        user.setBirthday(LocalDate.now().minusYears(1));

        User created = userController.create(user);

        User updated = new User();
        updated.setId(created.getId());
        updated.setName("Updated Name");

        User result = userController.update(updated);

        assertEquals("Updated Name", result.getName());
        assertNull(result.getEmail());
        assertNull(result.getLogin());
        assertNull(result.getBirthday());
    }

    @Test
    void shouldUpdateUserSuccessfully() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setLogin("validlogin");
        user.setBirthday(LocalDate.now().minusYears(1));

        User created = userController.create(user);

        User updated = new User();
        updated.setId(created.getId());
        updated.setEmail("updated@example.com");
        updated.setLogin("updatedlogin");
        updated.setBirthday(LocalDate.now().minusYears(2));
        updated.setName("Updated Name");

        User result = userController.update(updated);

        assertEquals("updated@example.com", result.getEmail());
        assertEquals("updatedlogin", result.getLogin());
        assertEquals("Updated Name", result.getName());
    }

    @Test
    void shouldReturnAllUsers() {
        User user1 = new User();
        user1.setEmail("test1@example.com");
        user1.setLogin("login1");
        user1.setBirthday(LocalDate.now().minusYears(1));

        User user2 = new User();
        user2.setEmail("test2@example.com");
        user2.setLogin("login2");
        user2.setBirthday(LocalDate.now().minusYears(2));

        userController.create(user1);
        userController.create(user2);

        var allUsers = userController.getUsers();

        assertEquals(2, allUsers.size());
    }
}