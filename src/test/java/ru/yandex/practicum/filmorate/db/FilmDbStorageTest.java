package ru.yandex.practicum.filmorate.db;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.storage.db.FilmDbStorage;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class FilmDbStorageTest {
    private final FilmDbStorage filmStorage;
    private final JdbcTemplate jdbcTemplate;

    private Long firstFilmId;
    private Long secondFilmId;
    private Long thirdFilmId;
    private Long firstUserId;
    private Long secondUserId;
    private Long thirdUserId;

    @BeforeEach
    void setUp() {
        List<Long> filmIds = jdbcTemplate.queryForList(
                "SELECT id FROM films ORDER BY id", Long.class);

        if (!filmIds.isEmpty()) {
            firstFilmId = filmIds.get(0);
            secondFilmId = filmIds.size() > 1 ? filmIds.get(1) : null;
            thirdFilmId = filmIds.size() > 2 ? filmIds.get(2) : null;
        }

        List<Long> userIds = jdbcTemplate.queryForList(
                "SELECT id FROM users ORDER BY id", Long.class);

        if (!userIds.isEmpty()) {
            firstUserId = userIds.get(0);
            secondUserId = userIds.size() > 1 ? userIds.get(1) : null;
            thirdUserId = userIds.size() > 2 ? userIds.get(2) : null;
        }

        jdbcTemplate.update("DELETE FROM likes");
    }

    private MpaRating getMpaRatingFromDb(Integer id) {
        String sql = "SELECT id, name FROM mpa_ratings WHERE id = ?";
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) ->
                new MpaRating(rs.getInt("id"), rs.getString("name"), ""), id);
    }

    @Test
    void testGetExistingFilm() {
        Film film = filmStorage.get(firstFilmId);

        assertThat(film).isNotNull();
        assertThat(film.getName()).isEqualTo("Начало");
        assertThat(film.getDescription()).isEqualTo("Фильм о сновидениях.");
        assertThat(film.getReleaseDate()).isEqualTo(LocalDate.of(2010, 7, 16));
        assertThat(film.getDuration()).isEqualTo(148);
        assertThat(film.getMpa().getId()).isEqualTo(3);
    }

    @Test
    void testGetAllFilms() {
        List<Film> allFilms = filmStorage.getAll();

        assertThat(allFilms).hasSize(5);

        List<String> filmTitles = allFilms.stream()
                .map(Film::getName)
                .toList();

        assertThat(filmTitles).containsExactlyInAnyOrder(
                "Начало",
                "Крестный отец",
                "Побег из Шоушенка",
                "Интерстеллар",
                "Форрест Гамп"
        );
    }

    @Test
    void testAddAndRemoveLike() {
        assertThat(firstFilmId).isNotNull();
        assertThat(firstUserId).isNotNull();

        filmStorage.addLike(firstFilmId, firstUserId);

        List<Long> likes = filmStorage.getLikes(firstFilmId);
        assertThat(likes).hasSize(1);
        assertThat(likes.getFirst()).isEqualTo(firstUserId);

        filmStorage.removeLike(firstFilmId, firstUserId);

        likes = filmStorage.getLikes(firstFilmId);
        assertThat(likes).isEmpty();
    }

    @Test
    void testGetPopularFilms() {
        assertThat(firstUserId).isNotNull();
        assertThat(secondUserId).isNotNull();
        assertThat(thirdUserId).isNotNull();

        // Добавляем лайки разным фильмам
        filmStorage.addLike(firstFilmId, firstUserId);   // 1 лайк первому фильму
        filmStorage.addLike(firstFilmId, secondUserId);  // 2 лайка первому фильму
        filmStorage.addLike(firstFilmId, thirdUserId);   // 3 лайка первому фильму

        filmStorage.addLike(secondFilmId, firstUserId);  // 1 лайк второму фильму
        filmStorage.addLike(secondFilmId, secondUserId); // 2 лайка второму фильму

        filmStorage.addLike(thirdFilmId, firstUserId);   // 1 лайк третьему фильму

        List<Film> popularFilms = filmStorage.getPopularFilms(2);
        assertThat(popularFilms).hasSize(2);

        assertThat(popularFilms.get(0).getId()).isEqualTo(firstFilmId);
        assertThat(popularFilms.get(1).getId()).isEqualTo(secondFilmId);

        List<Film> allPopularFilms = filmStorage.getPopularFilms(10);
        assertThat(allPopularFilms).hasSize(5);

        assertThat(allPopularFilms.get(0).getId()).isEqualTo(firstFilmId);
        assertThat(allPopularFilms.get(1).getId()).isEqualTo(secondFilmId);
    }

    @Test
    void testCreateFilm() {
        Film newFilm = Film.builder()
                .name("Новый фильм")
                .description("Описание нового фильма")
                .releaseDate(LocalDate.of(2023, 1, 1))
                .duration(120)
                .mpa(getMpaRatingFromDb(3))
                .build();

        Film created = filmStorage.create(newFilm);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo("Новый фильм");

        Film retrieved = filmStorage.get(created.getId());
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getMpa().getId()).isEqualTo(3);
    }

    @Test
    void testUpdateFilm() {
        Film film = filmStorage.get(firstFilmId);
        assertThat(film).isNotNull();

        film.setName("Обновленное название");
        film.setDescription("Обновленное описание");

        Film updated = filmStorage.update(film);

        assertThat(updated.getId()).isEqualTo(firstFilmId);
        assertThat(updated.getName()).isEqualTo("Обновленное название");

        Film fromDb = filmStorage.get(firstFilmId);
        assertThat(fromDb.getName()).isEqualTo("Обновленное название");
    }

    @Test
    void testFilmWithGenres() {
        String genreSql = "SELECT id, name FROM genres WHERE id IN (1, 2)";
        List<Genre> genres = jdbcTemplate.query(genreSql, (rs, rowNum) ->
                new Genre(rs.getInt("id"), rs.getString("name")));

        Film film = Film.builder()
                .name("Фильм с жанрами")
                .description("Тестируем жанры")
                .releaseDate(LocalDate.of(2023, 1, 1))
                .duration(120)
                .mpa(getMpaRatingFromDb(3))
                .genres(new HashSet<>(genres))
                .build();

        Film created = filmStorage.create(film);

        Film retrieved = filmStorage.get(created.getId());
        assertThat(retrieved.getGenres()).hasSize(2);
    }

    @Test
    void testDeleteFilm() {
        Film filmToDelete = Film.builder()
                .name("Фильм для удаления")
                .description("Будет удален")
                .releaseDate(LocalDate.of(2023, 1, 1))
                .duration(100)
                .mpa(getMpaRatingFromDb(1))
                .build();

        Film created = filmStorage.create(filmToDelete);
        Long idToDelete = created.getId();

        boolean deleted = filmStorage.delete(idToDelete);
        assertThat(deleted).isTrue();

        Film afterDelete = filmStorage.get(idToDelete);
        assertThat(afterDelete).isNull();
    }

    @Test
    void testGetLikes() {
        filmStorage.addLike(firstFilmId, firstUserId);
        filmStorage.addLike(firstFilmId, secondUserId);
        filmStorage.addLike(firstFilmId, thirdUserId);

        List<Long> likes = filmStorage.getLikes(firstFilmId);
        assertThat(likes).hasSize(3);
        assertThat(likes).containsExactlyInAnyOrder(firstUserId, secondUserId, thirdUserId);
    }

    @Test
    void testRemoveNonExistentLike() {
        assertThat(firstFilmId).isNotNull();
        assertThat(firstUserId).isNotNull();

        filmStorage.removeLike(firstFilmId, firstUserId);

        List<Long> likes = filmStorage.getLikes(firstFilmId);
        assertThat(likes).isEmpty();
    }

    @Test
    void testGetNonExistentFilm() {
        Film film = filmStorage.get(999999L);
        assertThat(film).isNull();
    }

    @Test
    void testUpdateNonExistentFilm() {
        Film nonExistentFilm = Film.builder()
                .id(999999L)
                .name("Несуществующий")
                .description("Нет в БД")
                .releaseDate(LocalDate.now())
                .duration(100)
                .mpa(getMpaRatingFromDb(1))
                .build();

        Film updated = filmStorage.update(nonExistentFilm);
        assertThat(updated).isNull();
    }
}