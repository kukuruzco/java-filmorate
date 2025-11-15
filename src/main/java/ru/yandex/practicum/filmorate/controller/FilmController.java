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
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/films")
public class FilmController {
    LocalDate minReleaseDate = LocalDate.of(1895, 12, 28);
    private final Map<Long, Film> films = new HashMap<>();

    @GetMapping
    public Collection<Film> getFilms() {
        log.info("Получен запрос на получение всех фильмов");
        return films.values();
    }

    @PostMapping
    public Film create(@Valid @RequestBody Film newFilm) {
        log.info("Получен запрос на создание фильма: {}", newFilm);

        if (newFilm.getReleaseDate() != null && newFilm.getReleaseDate().isBefore(minReleaseDate)) {
            log.warn("Дата релиза раньше 28 декабря 1895 года: {}", newFilm.getReleaseDate());
            throw new ValidationException("Дата релиза не может быть раньше 28 декабря 1895 года");
        }

        newFilm.setId(getNextId());
        films.put(newFilm.getId(), newFilm);

        log.info("Фильм создан с id: {}", newFilm.getId());
        return newFilm;
    }

    @PutMapping
    public Film update(@Valid @RequestBody Film updatedFilm) {
        log.info("Получен запрос на обновление фильма: {}", updatedFilm);

        if (updatedFilm.getId() == null) {
            log.warn("Id фильма не указан");
            throw new ValidationException("Id должен быть указан");
        }

        if (updatedFilm.getReleaseDate() != null && updatedFilm.getReleaseDate().isBefore(minReleaseDate)) {
            log.warn("Дата релиза раньше 28 декабря 1895 года: {}", updatedFilm.getReleaseDate());
            throw new ValidationException("Дата релиза не может быть раньше 28 декабря 1895 года");
        }

        if (!films.containsKey(updatedFilm.getId())) {
            log.warn("Фильм с id = {} не найден", updatedFilm.getId());
            throw new NotFoundException("Фильм с id = " + updatedFilm.getId() + " не найден");
        }

        films.put(updatedFilm.getId(), updatedFilm);

        log.info("Фильм с id = {} обновлён", updatedFilm.getId());
        return updatedFilm;
    }

    private long getNextId() {
        long currentMaxId = films.keySet()
                .stream()
                .mapToLong(id -> id)
                .max()
                .orElse(0);
        return ++currentMaxId;
    }
}