package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FilmControllerTest {

    private FilmController filmController;

    @BeforeEach
    void setUp() {
        filmController = new FilmController();
    }

    @Test
    void shouldGenerateIdWhenCreatingFilm() {
        Film film = new Film();
        film.setName("Test Film");
        film.setDescription("A test film");
        film.setReleaseDate(LocalDate.of(2020, 1, 1));
        film.setDuration(120);

        Film result = filmController.create(film);

        assertNotNull(result.getId());
        assertEquals(1L, result.getId());
    }

    @Test
    void shouldIncrementIdForMultipleFilms() {
        Film film1 = new Film();
        film1.setName("Test Film 1");
        film1.setDescription("A test film");
        film1.setReleaseDate(LocalDate.of(2020, 1, 1));
        film1.setDuration(120);

        Film film2 = new Film();
        film2.setName("Test Film 2");
        film2.setDescription("Another test film");
        film2.setReleaseDate(LocalDate.of(2021, 1, 1));
        film2.setDuration(150);

        Film result1 = filmController.create(film1);
        Film result2 = filmController.create(film2);

        assertEquals(1L, result1.getId());
        assertEquals(2L, result2.getId());
    }

    @Test
    void shouldThrowValidationExceptionWhenReleaseDateIsBeforeMinDate() {
        Film film = new Film();
        film.setName("Test Film");
        film.setReleaseDate(LocalDate.of(1890, 1, 1)); // раньше 1895
        film.setDuration(120);

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> filmController.create(film)
        );
        assertTrue(exception.getMessage().contains("1895"));
    }

    @Test
    void shouldThrowValidationExceptionWhenIdIsNullInUpdate() {
        Film film = new Film();
        film.setId(null);

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> filmController.update(film)
        );
        assertTrue(exception.getMessage().contains("Id должен быть указан"));
    }

    @Test
    void shouldThrowNotFoundExceptionWhenFilmDoesNotExistInUpdate() {
        Film film = new Film();
        film.setId(999L);
        film.setName("Test Film");
        film.setReleaseDate(LocalDate.of(2020, 1, 1));
        film.setDuration(120);

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> filmController.update(film)
        );
        assertTrue(exception.getMessage().contains("не найден"));
    }

    @Test
    void shouldReplaceFilmEntirely() {
        Film film = new Film();
        film.setName("Original Film");
        film.setDescription("Original Description");
        film.setReleaseDate(LocalDate.of(2020, 1, 1));
        film.setDuration(120);

        Film created = filmController.create(film);

        Film updated = new Film();
        updated.setId(created.getId());
        updated.setName("Updated Film");

        Film result = filmController.update(updated);

        assertEquals("Updated Film", result.getName());
        assertNull(result.getDescription());
        assertNull(result.getReleaseDate());
        assertNull(result.getDuration());
    }

    @Test
    void shouldUpdateFilmSuccessfully() {
        Film film = new Film();
        film.setName("Original Film");
        film.setDescription("Original Description");
        film.setReleaseDate(LocalDate.of(2020, 1, 1));
        film.setDuration(120);

        Film created = filmController.create(film);

        Film updated = new Film();
        updated.setId(created.getId());
        updated.setName("Updated Film");
        updated.setDescription("Updated Description");
        updated.setReleaseDate(LocalDate.of(2021, 1, 1));
        updated.setDuration(150);

        Film result = filmController.update(updated);

        assertEquals("Updated Film", result.getName());
        assertEquals("Updated Description", result.getDescription());
        assertEquals(LocalDate.of(2021, 1, 1), result.getReleaseDate());
        assertEquals(150, result.getDuration());
    }

    @Test
    void shouldReturnAllFilms() {
        Film film1 = new Film();
        film1.setName("Film 1");
        film1.setReleaseDate(LocalDate.of(2020, 1, 1));
        film1.setDuration(120);

        Film film2 = new Film();
        film2.setName("Film 2");
        film2.setReleaseDate(LocalDate.of(2021, 1, 1));
        film2.setDuration(150);

        filmController.create(film1);
        filmController.create(film2);

        var allFilms = filmController.getFilms();

        assertEquals(2, allFilms.size());
    }
}