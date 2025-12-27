package ru.yandex.practicum.filmorate.db;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Transactional
class UserDbStorageTest {
    private final UserDbStorage userStorage;
    private final JdbcTemplate jdbcTemplate;

    private Long firstUserId;
    private Long secondUserId;
    private Long thirdUserId;
    private Long fourthUserId;

    @BeforeEach
    void setUp() {
        List<Long> userIds = jdbcTemplate.queryForList(
                "SELECT id FROM users ORDER BY email", Long.class);

        if (!userIds.isEmpty()) {
            firstUserId = userIds.get(0);
            secondUserId = userIds.size() > 1 ? userIds.get(1) : null;
            thirdUserId = userIds.size() > 2 ? userIds.get(2) : null;
            fourthUserId = userIds.size() > 3 ? userIds.get(3) : null;
        }

        System.out.println("DEBUG: User IDs - first: " + firstUserId +
                ", second: " + secondUserId +
                ", third: " + thirdUserId +
                ", fourth: " + fourthUserId);

        Integer friendshipsCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM friendships", Integer.class);
        System.out.println("DEBUG: Friendships in DB: " + friendshipsCount);
    }

    @Test
    void testGetExistingUser() {
        User user = userStorage.get(firstUserId);

        assertThat(user).isNotNull();
        assertThat(user.getId()).isEqualTo(firstUserId);
        assertThat(user.getEmail()).isEqualTo("user1@example.com");
        assertThat(user.getLogin()).isEqualTo("user1");
        assertThat(user.getName()).isEqualTo("Анна Иванова");
        assertThat(user.getBirthday()).isEqualTo(LocalDate.of(1990, 5, 15));
    }

    @Test
    void testGetNonExistentUser() {
        User user = userStorage.get(999L);
        assertThat(user).isNull();
    }

    @Test
    void testCreateNewUser() {
        User newUser = User.builder()
                .email("newuser@example.com")
                .login("newlogin")
                .name("Новый Пользователь")
                .birthday(LocalDate.of(1998, 7, 20))
                .build();

        User created = userStorage.create(newUser);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getEmail()).isEqualTo("newuser@example.com");
        assertThat(created.getLogin()).isEqualTo("newlogin");

        User retrieved = userStorage.get(created.getId());
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getName()).isEqualTo("Новый Пользователь");
    }

    @Test
    void testUpdateUser() {
        User userToUpdate = userStorage.get(secondUserId);
        assertThat(userToUpdate).isNotNull();

        userToUpdate.setName("Петр Обновленный");
        userToUpdate.setEmail("updated@example.com");

        User updated = userStorage.update(userToUpdate);

        assertThat(updated.getId()).isEqualTo(secondUserId);
        assertThat(updated.getName()).isEqualTo("Петр Обновленный");
        assertThat(updated.getEmail()).isEqualTo("updated@example.com");

        User fromDb = userStorage.get(secondUserId);
        assertThat(fromDb.getName()).isEqualTo("Петр Обновленный");
    }

    @Test
    void testGetAllUsers() {
        List<User> allUsers = userStorage.getAll();

        assertThat(allUsers).hasSize(4);

        List<Long> userIds = allUsers.stream()
                .map(User::getId)
                .toList();

        assertThat(userIds).containsExactlyInAnyOrder(
                firstUserId, secondUserId, thirdUserId, fourthUserId);
    }

    @Test
    void testUserExists() {
        boolean exists = userStorage.exists(thirdUserId);
        assertThat(exists).isTrue();

        boolean notExists = userStorage.exists(99L);
        assertThat(notExists).isFalse();
    }

    @Test
    void testUserFriendsStructure() {

        List<User> friendsOfUser1 = userStorage.getFriendsList(firstUserId);
        assertThat(friendsOfUser1).hasSize(2);

        List<User> friendsOfUser2 = userStorage.getFriendsList(secondUserId);
        assertThat(friendsOfUser2).hasSize(3);

        List<User> friendsOfUser4 = userStorage.getFriendsList(fourthUserId);
        assertThat(friendsOfUser4).hasSize(1);

        User user1 = userStorage.get(firstUserId);
        assertThat(user1).isNotNull();
    }

    @Test
    void testDeleteUser() {
        boolean deleted = userStorage.delete(firstUserId);
        assertThat(deleted).isTrue();

        User afterDelete = userStorage.get(firstUserId);
        assertThat(afterDelete).isNull();

        boolean stillExists = userStorage.exists(firstUserId);
        assertThat(stillExists).isFalse();
    }

    @Test
    void testCreateUserWithInvalidData() {

        User invalidUser = User.builder()
                .email("")
                .login("")
                .name("")
                .birthday(LocalDate.of(3000, 1, 1))  // дата в будущем
                .build();
    }

    @Test
    void testUpdateNonExistentUser() {
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