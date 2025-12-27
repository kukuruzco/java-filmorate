package ru.yandex.practicum.filmorate.service;

import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.db.FilmDbStorage;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class FilmService {
    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private final FilmDbStorage filmDbStorage;
    private JdbcTemplate jdbcTemplate;
    private final LocalDate minReleaseDate = LocalDate.of(1895, 12, 28);

    public FilmService(FilmStorage filmStorage, UserStorage userStorage, FilmDbStorage filmDbStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
        this.filmDbStorage = filmDbStorage;
        this.jdbcTemplate = null;
    }

    @Autowired
    public FilmService(FilmStorage filmStorage, UserStorage userStorage, FilmDbStorage filmDbStorage, JdbcTemplate jdbcTemplate) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
        this.filmDbStorage = filmDbStorage;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Film> getAll() {
        log.info("Получен запрос на получение всех фильмов");
        return filmStorage.getAll();
    }

    public Film getById(Long id) {
        Film film = filmStorage.get(id);
        if (film == null) {
            throw new NotFoundException("Фильм с id = " + id + " не найден");
        }
        return film;
    }

    public Film create(Film film) {
        log.info("Получен запрос на создание фильма: {}", film);
        validateFilm(film);
        validateMpaExists(film.getMpa());
        validateGenresExist(film.getGenres());
        return filmStorage.create(film);
    }

    public Film update(Film film) {
        log.info("Получен запрос на обновление фильма: {}", film);
        validateFilm(film);
        isValidFilm(film);
        validateMpaExists(film.getMpa());
        validateGenresExist(film.getGenres());
        return filmStorage.update(film);
    }

    public void addLike(Long filmId, Long userId) {
        log.info("Получен запрос на добавление лайка фильму {} от пользователя {}", filmId, userId);

        getFilmOrThrow(filmId);
        getUserOrThrow(userId);

        filmDbStorage.addLike(filmId, userId);
        log.info("Пользователь {} поставил лайк фильму {}", userId, filmId);
    }

    public void removeLike(Long filmId, Long userId) {
        log.info("Получен запрос на удаление лайка фильму {} от пользователя {}", filmId, userId);

        getFilmOrThrow(filmId);
        getUserOrThrow(userId);

        filmDbStorage.removeLike(filmId, userId);
        log.info("Пользователь {} удалил лайк фильму {}", userId, filmId);
    }

    public List<Film> getPopularFilms(int count) {
        log.info("Получен запрос на получение {} популярных фильмов", count);
        return filmDbStorage.getPopularFilms(count);
    }

    private void validateFilm(Film film) {
        if (film.getReleaseDate() != null && film.getReleaseDate().isBefore(minReleaseDate)) {
            log.warn("Дата релиза раньше 28 декабря 1895 года: {}", film.getReleaseDate());
            throw new IllegalArgumentException("Дата релиза не может быть раньше 28 декабря 1895 года");
        }
    }

    private void validateMpaExists(MpaRating mpa) {
        if (mpa == null) {
            log.warn("MPA рейтинг не указан");
            throw new ValidationException("MPA рейтинг обязателен");
        }

        if (jdbcTemplate == null) {
            if (mpa.getId() == null) {
                throw new ValidationException("ID MPA рейтинга обязателен");
            }
            if (mpa.getId() < 1 || mpa.getId() > 5) {
                throw new NotFoundException("MPA рейтинг с id=" + mpa.getId() + " не найден");
            }
            log.info("Тестовая проверка MPA: id={} существует", mpa.getId());
        } else {
            String sql = "SELECT COUNT(*) FROM mpa_ratings WHERE id = ?";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, mpa.getId());

            if (count == 0) {
                throw new NotFoundException("MPA рейтинг с id=" + mpa.getId() + " не найден");
            }
            log.info("Проверка MPA через БД: id={} существует", mpa.getId());
        }
    }

    private void validateGenresExist(Set<Genre> genres) {
        if (genres == null || genres.isEmpty()) {
            return;
        }

        if (jdbcTemplate == null) {
            for (Genre genre : genres) {
                if (genre.getId() < 1 || genre.getId() > 6) {
                    throw new NotFoundException("Жанр с id=" + genre.getId() + " не найден");
                }
            }
            log.info("Тестовая проверка жанров: все жанры существуют");
        } else {
            for (Genre genre : genres) {
                String sql = "SELECT COUNT(*) FROM genres WHERE id = ?";
                Integer count = jdbcTemplate.queryForObject(sql, Integer.class, genre.getId());

                if (count == 0) {
                    throw new NotFoundException("Жанр с id=" + genre.getId() + " не найден");
                }
            }
            log.info("Проверка жанров через БД: все жанры существуют");
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
}