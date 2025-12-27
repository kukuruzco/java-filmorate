package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.db.FilmDbStorage;
import ru.yandex.practicum.filmorate.db.GenreDbStorage;
import ru.yandex.practicum.filmorate.db.MpaRatingDbStorage;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.storage.InMemoryFilmStorage;
import ru.yandex.practicum.filmorate.storage.InMemoryUserStorage;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class FilmValidationTest {

    private FilmController filmController;
    private LocalDate minReleaseDate;
    private Validator validator;

    static class TestJdbcTemplate extends JdbcTemplate {
    }

    static class TestMpaRatingDbStorage extends MpaRatingDbStorage {
        public TestMpaRatingDbStorage() {
            super(new TestJdbcTemplate());
        }

        @Override
        public MpaRating getById(Integer id) {
            return switch (id) {
                case 1 -> new MpaRating(1, "G", "у фильма нет возрастных ограничений");
                case 2 -> new MpaRating(2, "PG", "детям рекомендуется смотреть фильм с родителями");
                case 3 -> new MpaRating(3, "PG-13", "детям до 13 лет просмотр не желателен");
                case 4 ->
                        new MpaRating(4, "R", "лицам до 17 лет просматривать фильм можно только в присутствии взрослого");
                case 5 -> new MpaRating(5, "NC-17", "лицам до 18 лет просмотр запрещён");
                default -> null;
            };
        }
    }

    static class TestGenreDbStorage extends GenreDbStorage {
        public TestGenreDbStorage() {
            super(new TestJdbcTemplate());
        }

        @Override
        public Genre getById(Integer id) {
            // Возвращаем тестовые жанры
            return switch (id) {
                case 1 -> new Genre(1, "Комедия");
                case 2 -> new Genre(2, "Драма");
                case 3 -> new Genre(3, "Мультфильм");
                case 4 -> new Genre(4, "Триллер");
                case 5 -> new Genre(5, "Документальный");
                case 6 -> new Genre(6, "Боевик");
                default -> null;
            };
        }
    }

    static class TestFilmDbStorage extends FilmDbStorage {
        private final InMemoryFilmStorage memoryStorage = new InMemoryFilmStorage();
        private final Map<Long, Set<Long>> likes = new HashMap<>();
        private long nextId = 1;

        public TestFilmDbStorage() {
            super(new TestJdbcTemplate(), new TestMpaRatingDbStorage(), new TestGenreDbStorage());
        }

        @Override
        public List<Film> getAll() {
            return memoryStorage.getAll();
        }

        @Override
        public Film get(Long id) {
            return memoryStorage.get(id);
        }

        @Override
        public Film create(Film film) {
            if (film.getId() == null) {
                film.setId(nextId++);
            }
            if (film.getMpa() != null && film.getMpa().getId() != null) {
                MpaRating mpa = getMpaRatingStorage().getById(film.getMpa().getId());
                film.setMpa(mpa);
            }
            return memoryStorage.create(film);
        }

        @Override
        public Film update(Film film) {
            if (film.getMpa() != null && film.getMpa().getId() != null) {
                MpaRating mpa = getMpaRatingStorage().getById(film.getMpa().getId());
                film.setMpa(mpa);
            }
            return memoryStorage.update(film);
        }

        @Override
        public boolean delete(Long id) {
            return memoryStorage.delete(id);
        }

        @Override
        public boolean exists(Long id) {
            return memoryStorage.exists(id);
        }

        // Методы для лайков
        @Override
        public void addLike(Long filmId, Long userId) {
            likes.putIfAbsent(filmId, new HashSet<>());
            likes.get(filmId).add(userId);
        }

        @Override
        public void removeLike(Long filmId, Long userId) {
            if (likes.containsKey(filmId)) {
                likes.get(filmId).remove(userId);
            }
        }

        @Override
        public List<Film> getPopularFilms(int count) {
            return memoryStorage.getAll().stream()
                    .sorted((f1, f2) -> {
                        int likes1 = likes.getOrDefault(f1.getId(), Collections.emptySet()).size();
                        int likes2 = likes.getOrDefault(f2.getId(), Collections.emptySet()).size();
                        return Integer.compare(likes2, likes1);
                    })
                    .limit(Math.max(0, count))
                    .toList();
        }

        @Override
        public List<Long> getLikes(Long filmId) {
            return new ArrayList<>(likes.getOrDefault(filmId, Collections.emptySet()));
        }

        private TestMpaRatingDbStorage getMpaRatingStorage() {
            return (TestMpaRatingDbStorage) super.mpaRatingDbStorage;
        }
    }

    @BeforeEach
    void setUp() {
        TestFilmDbStorage testFilmDbStorage = new TestFilmDbStorage();

        FilmService filmService = new FilmService(
                testFilmDbStorage,          // как FilmStorage
                new InMemoryUserStorage(),
                testFilmDbStorage           // как FilmDbStorage
        );

        filmController = new FilmController(filmService);
        minReleaseDate = LocalDate.of(1895, 12, 28);

        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void whenCreateValidFilm_thenFilmCreated() {
        Film film = createValidFilm();

        Film createdFilm = filmController.create(film);

        assertNotNull(createdFilm.getId());
        assertEquals(film.getName(), createdFilm.getName());
        assertEquals(film.getDescription(), createdFilm.getDescription());
        assertEquals(film.getReleaseDate(), createdFilm.getReleaseDate());
        assertEquals(film.getDuration(), createdFilm.getDuration());
    }

    @Test
    void whenCreateFilmWithReleaseDateBeforeMinDate_thenThrowValidationException() {
        Film film = createValidFilm();
        film.setReleaseDate(minReleaseDate.minusDays(1));

        assertThrows(IllegalArgumentException.class, () -> filmController.create(film));
    }

    @Test
    void whenCreateFilmWithMinReleaseDate_thenFilmCreated() {
        Film film = createValidFilm();
        film.setReleaseDate(minReleaseDate);

        Film createdFilm = filmController.create(film);

        assertNotNull(createdFilm.getId());
        assertEquals(minReleaseDate, createdFilm.getReleaseDate());
    }

    @Test
    void whenCreateFilmWithNullReleaseDate_thenFilmCreated() {
        Film film = createValidFilm();
        film.setReleaseDate(null);

        Film createdFilm = filmController.create(film);

        assertNotNull(createdFilm.getId());
        assertNull(createdFilm.getReleaseDate());
    }

    @Test
    void whenUpdateValidFilm_thenFilmUpdated() {
        Film film = createValidFilm();
        Film createdFilm = filmController.create(film);

        Film updatedFilm = createValidFilm();
        updatedFilm.setId(createdFilm.getId());
        updatedFilm.setName("Updated Film");
        updatedFilm.setDescription("Updated description");

        Film result = filmController.update(updatedFilm);

        assertEquals(updatedFilm.getName(), result.getName());
        assertEquals(updatedFilm.getDescription(), result.getDescription());
    }

    @Test
    void whenUpdateFilmWithNullId_thenThrowValidationException() {
        Film film = createValidFilm();
        film.setId(null);

        assertThrows(IllegalArgumentException.class, () -> filmController.update(film));
    }

    @Test
    void whenUpdateNonExistentFilm_thenThrowNotFoundException() {
        Film film = createValidFilm();
        film.setId(999L);

        assertThrows(NotFoundException.class, () -> filmController.update(film));
    }

    @Test
    void whenUpdateFilmWithReleaseDateBeforeMinDate_thenThrowValidationException() {
        Film film = createValidFilm();
        Film createdFilm = filmController.create(film);

        Film updatedFilm = createValidFilm();
        updatedFilm.setId(createdFilm.getId());
        updatedFilm.setReleaseDate(minReleaseDate.minusDays(1));

        assertThrows(IllegalArgumentException.class, () -> filmController.update(updatedFilm));
    }

    @Test
    void whenGetAllFilms_thenReturnAllFilms() {
        Film film1 = createValidFilm();
        Film film2 = createValidFilm();
        film2.setName("Another Film");
        film2.setDescription("Another description");

        filmController.create(film1);
        filmController.create(film2);

        Collection<Film> films = filmController.getFilms();

        assertEquals(2, films.size());
        assertTrue(films.stream().anyMatch(f -> f.getName().equals(film1.getName())));
        assertTrue(films.stream().anyMatch(f -> f.getName().equals(film2.getName())));
    }

    @Test
    void whenCreateMultipleFilms_thenIdsAreIncremented() {
        Film film1 = createValidFilm();
        Film film2 = createValidFilm();
        film2.setName("Second Film");

        Film created1 = filmController.create(film1);
        Film created2 = filmController.create(film2);

        assertEquals(1L, created1.getId());
        assertEquals(2L, created2.getId());
    }

    @Test
    void whenGetEmptyFilmsList_thenReturnEmptyList() {
        Collection<Film> films = filmController.getFilms();

        assertTrue(films.isEmpty());
    }

    @Test
    void whenUpdateFilmWithNullReleaseDate_thenFilmUpdated() {
        Film film = createValidFilm();
        Film createdFilm = filmController.create(film);

        Film updatedFilm = createValidFilm();
        updatedFilm.setId(createdFilm.getId());
        updatedFilm.setReleaseDate(null);

        Film result = filmController.update(updatedFilm);

        assertNull(result.getReleaseDate());
    }

    @Test
    void whenValidFilm_thenNoViolations() {
        Film film = createValidFilm();

        Set<ConstraintViolation<Film>> violations = validator.validate(film);

        assertTrue(violations.isEmpty());
    }

    @Test
    void whenBlankName_thenViolation() {
        Film film = createValidFilm();
        film.setName("   ");

        Set<ConstraintViolation<Film>> violations = validator.validate(film);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("name")));
    }

    @Test
    void whenNullName_thenViolation() {
        Film film = createValidFilm();
        film.setName(null);

        Set<ConstraintViolation<Film>> violations = validator.validate(film);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("name")));
    }

    @Test
    void whenDescriptionTooLong_thenViolation() {
        Film film = createValidFilm();
        film.setDescription("A".repeat(201));

        Set<ConstraintViolation<Film>> violations = validator.validate(film);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("description")));
    }

    @Test
    void whenNegativeDuration_thenViolation() {
        Film film = createValidFilm();
        film.setDuration(-10);

        Set<ConstraintViolation<Film>> violations = validator.validate(film);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("duration")));
    }

    @Test
    void whenZeroDuration_thenViolation() {
        Film film = createValidFilm();
        film.setDuration(0);

        Set<ConstraintViolation<Film>> violations = validator.validate(film);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("duration")));
    }

    @Test
    void whenNullReleaseDate_thenViolation() {
        Film film = createValidFilm();
        film.setReleaseDate(null);

        Set<ConstraintViolation<Film>> violations = validator.validate(film);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("releaseDate")));
    }

    @Test
    void whenReleaseDateIsMinDate_thenNoViolation() {
        Film film = createValidFilm();
        film.setReleaseDate(LocalDate.of(1895, 12, 28));

        Set<ConstraintViolation<Film>> violations = validator.validate(film);

        assertTrue(violations.isEmpty());
    }

    @Test
    void whenReleaseDateIsAfterMinDate_thenNoViolation() {
        Film film = createValidFilm();
        film.setReleaseDate(LocalDate.of(1895, 12, 29));

        Set<ConstraintViolation<Film>> violations = validator.validate(film);

        assertTrue(violations.isEmpty());
    }

    @Test
    void whenPositiveDuration_thenNoViolation() {
        Film film = createValidFilm();
        film.setDuration(1);

        Set<ConstraintViolation<Film>> violations = validator.validate(film);

        assertTrue(violations.isEmpty());
    }

    @Test
    void whenDescriptionIsExactly200Characters_thenNoViolation() {
        Film film = createValidFilm();
        film.setDescription("A".repeat(200));

        Set<ConstraintViolation<Film>> violations = validator.validate(film);

        assertTrue(violations.isEmpty());
    }

    @Test
    void whenDescriptionIs199Characters_thenNoViolation() {
        Film film = createValidFilm();
        film.setDescription("A".repeat(199));

        Set<ConstraintViolation<Film>> violations = validator.validate(film);

        assertTrue(violations.isEmpty());
    }

    @Test
    void whenEmptyDescription_thenNoViolation() {
        Film film = createValidFilm();
        film.setDescription("");

        Set<ConstraintViolation<Film>> violations = validator.validate(film);

        assertTrue(violations.isEmpty());
    }

    @Test
    void whenNullDescription_thenNoViolation() {
        Film film = createValidFilm();
        film.setDescription(null);

        Set<ConstraintViolation<Film>> violations = validator.validate(film);

        assertTrue(violations.isEmpty());
    }

    private Film createValidFilm() {
        MpaRating mpa = new MpaRating(1, "G", "у фильма нет возрастных ограничений");

        Film film = new Film();
        film.setName("Valid Film");
        film.setDescription("Valid description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);
        film.setMpa(mpa);

        return film;
    }
}