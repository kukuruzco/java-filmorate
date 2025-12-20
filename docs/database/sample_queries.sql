-- ============================================
-- SAMPLE QUERIES для Filmorate Database (PostgreSQL)
-- ============================================

-- 1. ПОЛЬЗОВАТЕЛИ
-- ================

-- 1.1 Получить всех пользователей
SELECT id, email, login, name, birthday
FROM users
ORDER BY id;

-- 1.2 Найти пользователя по email
SELECT * FROM users WHERE email = 'user@example.com';

-- 1.3 Найти пользователей по логину
SELECT * FROM users WHERE login ILIKE 'john%';

-- 1.4 Пользователи, у которых сегодня день рождения
SELECT * FROM users
WHERE EXTRACT(MONTH FROM birthday) = EXTRACT(MONTH FROM CURRENT_DATE)
  AND EXTRACT(DAY FROM birthday) = EXTRACT(DAY FROM CURRENT_DATE);

-- 1.5 Количество пользователей
SELECT COUNT(*) as total_users FROM users;


-- 2. ФИЛЬМЫ
-- ==========

-- 2.1 Получить все фильмы с жанрами
SELECT
    f.id,
    f.title,
    f.description,
    f.release_date,
    f.duration,
    f.mpa_rating,
    ARRAY_AGG(g.name ORDER BY g.name) as genres
FROM films f
LEFT JOIN film_genres fg ON f.id = fg.film_id
LEFT JOIN genres g ON fg.genre_id = g.id
GROUP BY f.id, f.title, f.description, f.release_date, f.duration, f.mpa_rating
ORDER BY f.release_date DESC;

-- 2.2 Найти фильмы по названию
SELECT * FROM films WHERE title ILIKE '%matrix%';

-- 2.3 Фильмы определенного жанра
SELECT f.*
FROM films f
JOIN film_genres fg ON f.id = fg.film_id
JOIN genres g ON fg.genre_id = g.id
WHERE g.name = 'Комедия'
ORDER BY f.release_date DESC;

-- 2.4 Фильмы с рейтингом PG-13
SELECT * FROM films WHERE mpa_rating = 'PG-13';

-- 2.5 Новые фильмы (за последний год)
SELECT * FROM films
WHERE release_date >= CURRENT_DATE - INTERVAL '1 year'
ORDER BY release_date DESC;


-- 3. ЛАЙКИ И ПОПУЛЯРНОСТЬ
-- =======================

-- 3.1 Топ 10 самых популярных фильмов (по количеству лайков)
SELECT
    f.id,
    f.title,
    COUNT(l.film_id) as likes_count
FROM films f
LEFT JOIN likes l ON f.id = l.film_id
GROUP BY f.id, f.title
ORDER BY likes_count DESC, f.title
LIMIT 10;

-- 3.2 Фильмы, которые понравились конкретному пользователю
SELECT
    f.id,
    f.title,
    f.description,
    f.release_date,
    f.duration,
    f.mpa_rating
    ARRAY_AGG(g.name ORDER BY g.name) as genres
FROM films f
JOIN likes l ON f.id = l.film_id
LEFT JOIN film_genres fg ON f.id = fg.film_id
LEFT JOIN genres g ON fg.genre_id = g.id
WHERE l.user_id = 1
GROUP BY f.id, f.title, f.description, f.release_date, f.duration, f.mpa_rating;

-- 3.3 Пользователи, которым понравился конкретный фильм
SELECT u.*
FROM users u
JOIN likes l ON u.id = l.user_id
WHERE l.film_id = 5;


-- 4. ДРУЖБА
-- ===========

-- 4.1 Неподтвержденные запросы в друзья (входящие)
SELECT u.*,
       f.created_at as request_date
FROM users u
JOIN friendships f ON u.id = f.user_id
WHERE f.friend_id = 1 AND f.status = 'PENDING';

-- 4.2 Исходящие запросы на дружбу
SELECT u.*,
       f.created_at as request_date
FROM users u
JOIN friendships f ON u.id = f.friend_id
WHERE f.user_id = 1 AND f.status = 'PENDING';


-- 5. ЖАНРЫ
-- =========

-- 5.1 Все жанры
SELECT * FROM genres ORDER BY name;

-- 5.2 Самые популярные жанры (по количеству фильмов)
SELECT
    g.id,
    g.name,
    COUNT(fg.film_id) as film_count
FROM genres g
LEFT JOIN film_genres fg ON g.id = fg.genre_id
GROUP BY g.id, g.name
ORDER BY film_count DESC, g.name;