package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.storage.db.MpaRatingDbStorage;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/mpa")
@RequiredArgsConstructor
public class MpaController {
    private final MpaRatingDbStorage mpaStorage;

    @GetMapping
    public List<MpaRating> getAllMpa() {
        log.info("Получен запрос на получение всех рейтингов MPA");
        return mpaStorage.getAll();
    }

    @GetMapping("/{id}")
    public MpaRating getMpaById(@PathVariable Integer id) {
        log.info("Получен запрос на получение рейтинга MPA с id: {}", id);
        MpaRating mpa = mpaStorage.getById(id);
        if (mpa == null) {
            throw new NotFoundException("Рейтинг MPA с id = " + id + " не найден");
        }
        return mpa;
    }
}