package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FilmService {
    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private final LocalDate minReleaseDate = LocalDate.of(1895, 12, 28);

    private final Map<Long, Set<Long>> likes = new HashMap<>();

    public List<Film> getAll() {
        log.info("Получен запрос на получение всех фильмов");
        return filmStorage.getAll();
    }

    public Film create(Film film) {
        log.info("Получен запрос на создание фильма: {}", film);

        validateFilm(film);
        Film createdFilm = filmStorage.create(film);
        log.info("Фильм создан с id: {}", createdFilm.getId());
        return createdFilm;
    }

    public Film update(Film film) {
        log.info("Получен запрос на обновление фильма: {}", film);

        isValidFilm(film);

        validateFilm(film);
        Film updatedFilm = filmStorage.update(film);
        log.info("Фильм с id = {} обновлён", updatedFilm.getId());
        return updatedFilm;
    }

    public Film getById(Long id) {
        Film film = filmStorage.get(id);
        if (film == null) {
            throw new NotFoundException("Фильм с id = " + id + " не найден");
        }
        return film;
    }

    public void addLike(Long filmId, Long userId) {
        log.info("Получен запрос на добавление лайка фильму {} от пользователя {}", filmId, userId);

        Film film = getFilmOrThrow(filmId);
        getUserOrThrow(userId);

        likes.putIfAbsent(filmId, new HashSet<>());
        Set<Long> filmLikes = likes.get(filmId);

        if (filmLikes.contains(userId)) {
            log.warn("Пользователь {} уже поставил лайк фильму {}", userId, filmId);
            throw new IllegalArgumentException("Пользователь уже поставил лайк этому фильму");
        }

        filmLikes.add(userId);
        log.info("Пользователь {} поставил лайк фильму {}", userId, filmId);
    }

    public void removeLike(Long filmId, Long userId) {
        log.info("Получен запрос на удаление лайка фильму {} от пользователя {}", filmId, userId);

        Film film = getFilmOrThrow(filmId);
        getUserOrThrow(userId);

        if (!likes.containsKey(filmId) || !likes.get(filmId).contains(userId)) {
            log.warn("Пользователь {} не ставил лайк фильму {}", userId, filmId);
            throw new NotFoundException("Лайк не найден");
        }

        likes.get(filmId).remove(userId);
        log.info("Пользователь {} удалил лайк фильму {}", userId, filmId);
    }

    public List<Film> getPopularFilms(int count) {
        log.info("Получен запрос на получение {} популярных фильмов", count);

        return filmStorage.getAll().stream()
                .sorted((f1, f2) -> {
                    int likes1 = getLikesCount(f1.getId());
                    int likes2 = getLikesCount(f2.getId());
                    return Integer.compare(likes2, likes1);
                })
                .limit(count)
                .collect(Collectors.toList());
    }

    public int getLikesCount(Long filmId) {
        return likes.getOrDefault(filmId, Collections.emptySet()).size();
    }

    private void validateFilm(Film film) {
        if (film.getReleaseDate() != null && film.getReleaseDate().isBefore(minReleaseDate)) {
            log.warn("Дата релиза раньше 28 декабря 1895 года: {}", film.getReleaseDate());
            throw new IllegalArgumentException("Дата релиза не может быть раньше 28 декабря 1895 года");
        }
    }

    private Film getFilmOrThrow(Long filmId) {
        Film film = filmStorage.get(filmId);
        if (film == null) {
            throw new NotFoundException("Фильм с id = " + filmId + " не найден");
        }
        return film;
    }

    private void getUserOrThrow(Long userId) {
        if (!userStorage.exists(userId)) {
            throw new NotFoundException("Пользователь с id = " + userId + " не найден");
        }
    }

    private void isValidFilm(Film film) {
        if (film.getId() == null) {
            log.warn("Id фильма не указан");
            throw new IllegalArgumentException("Id должен быть указан");
        }

        if (!filmStorage.exists(film.getId())) {
            log.warn("Фильм с id = {} не найден", film.getId());
            throw new NotFoundException("Фильм с id = " + film.getId() + " не найден");
        }
    }
}
