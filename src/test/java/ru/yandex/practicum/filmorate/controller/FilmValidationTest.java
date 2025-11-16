package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FilmControllerTest {

    private FilmController filmController;
    private LocalDate minReleaseDate;
    private Validator validator;

    @BeforeEach
    void setUp() {
        filmController = new FilmController();
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

        assertThrows(ValidationException.class, () -> filmController.create(film));
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

        assertThrows(ValidationException.class, () -> filmController.update(film));
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

        assertThrows(ValidationException.class, () -> filmController.update(updatedFilm));
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
        Film film = new Film();
        film.setName("Valid Film");
        film.setDescription("Valid description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);
        return film;
    }
}