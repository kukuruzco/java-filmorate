package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * Film.
 */
@Data
public class Film {
    Long id;
    @NotBlank(message = "Название фильма не может быть пустым")
    String name;
    @Size(max = 200, message = "Максимальная длина описания — 200 символов")
    String description;
    @NotNull(message = "Дата релиза не может быть пустой")
    LocalDate releaseDate;
    @Positive(message = "Продолжительность фильма должна быть положительным числом")
    Integer duration;
}
