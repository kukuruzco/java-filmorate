package ru.yandex.practicum.filmorate.storage.db;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Sql(scripts = {
        "file:src/test/resources/schema.sql",
        "file:src/test/resources/data.sql"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class UserDbStorageTest {
    private final UserDbStorage userStorage;

    // Тест 1: Получение существующего пользователя
    @Test
    void testGetExistingUser() {
        User user = userStorage.get(1L);

        assertThat(user).isNotNull();
        assertThat(user.getId()).isEqualTo(1L);
        assertThat(user.getEmail()).isEqualTo("user1@example.com");
        assertThat(user.getLogin()).isEqualTo("user1");
        assertThat(user.getName()).isEqualTo("Анна Иванова");
        assertThat(user.getBirthday()).isEqualTo(LocalDate.of(1990, 5, 15));
    }

    // Тест 2: Получение несуществующего пользователя
    @Test
    void testGetNonExistentUser() {
        User user = userStorage.get(999L);
        assertThat(user).isNull();
    }

    // Тест 3: Создание нового пользователя
    @Test
    void testCreateNewUser() {
        User newUser = User.builder()
                .email("newuser@example.com")
                .login("newlogin")
                .name("Новый Пользователь")
                .birthday(LocalDate.of(1998, 7, 20))
                .build();

        User created = userStorage.create(newUser);

        // Должен получить ID 5, уже есть 4 пользователя
        assertThat(created.getId()).isEqualTo(5L);
        assertThat(created.getEmail()).isEqualTo("newuser@example.com");
        assertThat(created.getLogin()).isEqualTo("newlogin");

        // Проверяем, что пользователь действительно сохранен
        User retrieved = userStorage.get(5L);
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getName()).isEqualTo("Новый Пользователь");
    }

    // Тест 4: Обновление пользователя
    @Test
    void testUpdateUser() {
        User userToUpdate = userStorage.get(2L);
        assertThat(userToUpdate).isNotNull();

        // Обновляем данные
        userToUpdate.setName("Петр Обновленный");
        userToUpdate.setEmail("updated@example.com");

        User updated = userStorage.update(userToUpdate);

        assertThat(updated.getId()).isEqualTo(2L);
        assertThat(updated.getName()).isEqualTo("Петр Обновленный");
        assertThat(updated.getEmail()).isEqualTo("updated@example.com");

        User fromDb = userStorage.get(2L);
        assertThat(fromDb.getName()).isEqualTo("Петр Обновленный");
    }

    // Тест 5: Получение всех пользователей
    @Test
    void testGetAllUsers() {
        List<User> allUsers = userStorage.getAll();

        // Должно быть 4 пользователя из data.sql
        assertThat(allUsers).hasSize(4);

        List<Long> userIds = allUsers.stream()
                .map(User::getId)
                .toList();

        assertThat(userIds).containsExactlyInAnyOrder(1L, 2L, 3L, 4L);
    }

    // Тест 6: Проверка существования пользователя
    @Test
    void testUserExists() {
        boolean exists = userStorage.exists(3L);
        assertThat(exists).isTrue();

        boolean notExists = userStorage.exists(99L);
        assertThat(notExists).isFalse();
    }

    @Test
    void testUserFriendsStructure() {
        // У пользователя 1 должно быть 2 подтвержденных друга (2 и 3)
        List<User> friendsOfUser1 = userStorage.getFriendsList(1L);
        assertThat(friendsOfUser1).hasSize(2);

        // У пользователя 2 должно быть 2 друга (1 и 3 подтвержденные, 4 в ожидании)
        List<User> friendsOfUser2 = userStorage.getFriendsList(2L);
        assertThat(friendsOfUser2).hasSize(2); // только подтвержденные

        // У пользователя 4 должен быть 1 подтвержденный друг (3)
        List<User> friendsOfUser4 = userStorage.getFriendsList(4L);
        assertThat(friendsOfUser4).hasSize(1);

        User user1 = userStorage.get(1L);
        assertThat(user1).isNotNull();
    }

    @Test
    void testDeleteUser() {
        // Тест удаления пользователя
        boolean deleted = userStorage.delete(1L);
        assertThat(deleted).isTrue();

        // Проверяем, что пользователь удален
        User afterDelete = userStorage.get(1L);
        assertThat(afterDelete).isNull();

        boolean stillExists = userStorage.exists(1L);
        assertThat(stillExists).isFalse();
    }

    @Test
    void testCreateUserWithInvalidData() {
        // Тест создания пользователя с некорректными данными
        User invalidUser = User.builder()
                .email("")  // пустой email
                .login("")  // пустой логин
                .name("")
                .birthday(LocalDate.of(3000, 1, 1))  // дата в будущем
                .build();
    }

    @Test
    void testUpdateNonExistentUser() {
        // Попытка обновить несуществующего пользователя
        User nonExistent = User.builder()
                .id(999L)
                .email("ghost@mail.com")
                .login("ghost")
                .name("Ghost")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();

        User updated = userStorage.update(nonExistent);
        assertThat(updated).isNull();
    }
}