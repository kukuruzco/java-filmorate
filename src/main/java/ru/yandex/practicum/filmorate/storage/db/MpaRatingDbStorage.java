package ru.yandex.practicum.filmorate.storage.db;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.MpaRating;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class MpaRatingDbStorage {
    private final JdbcTemplate jdbcTemplate;

    public MpaRatingDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<MpaRating> getAll() {
        String sql = "SELECT * FROM mpa_ratings ORDER BY id";
        return jdbcTemplate.query(sql, this::mapRowToMpa);
    }

    public MpaRating getById(Integer id) {
        String sql = "SELECT * FROM mpa_ratings WHERE id = ?";
        List<MpaRating> ratings = jdbcTemplate.query(sql, this::mapRowToMpa, id);
        return ratings.isEmpty() ? null : ratings.getFirst();
    }

    public MpaRating getByCode(String code) {
        String sql = "SELECT * FROM mpa_ratings WHERE name = ?";
        List<MpaRating> ratings = jdbcTemplate.query(sql, this::mapRowToMpa, code);
        return ratings.isEmpty() ? null : ratings.get(0);
    }

    private MpaRating mapRowToMpa(ResultSet rs, int rowNum) throws SQLException {
        return new MpaRating(
                rs.getInt("id"),
                rs.getString("name"),
                getDescriptionByCode(rs.getString("name"))
        );
    }

    private String getDescriptionByCode(String code) {
        return switch (code) {
            case "G" -> "у фильма нет возрастных ограничений";
            case "PG" -> "детям рекомендуется смотреть фильм с родителями";
            case "PG-13" -> "детям до 13 лет просмотр не желателен";
            case "R" -> "лицам до 17 лет просматривать фильм можно только в присутствии взрослого";
            case "NC-17" -> "лицам до 18 лет просмотр запрещён";
            default -> "";
        };
    }
}