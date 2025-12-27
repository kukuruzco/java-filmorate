-- Тестовые данные
DELETE FROM likes;
DELETE FROM film_genres;
DELETE FROM films;
DELETE FROM friendships;
DELETE FROM users;

ALTER TABLE films ALTER COLUMN id RESTART WITH 1;
ALTER TABLE users ALTER COLUMN id RESTART WITH 1;

-- Тестовые пользователи
INSERT INTO users (email, login, name, birthday) VALUES
    ('user1@example.com', 'user1', 'Анна Иванова', '1990-05-15'),
    ('user2@example.com', 'user2', 'Петр Сидоров', '1985-08-22'),
    ('user3@example.com', 'user3', 'Мария Петрова', '1995-03-10'),
    ('user4@example.com', 'user4', 'Иван Козлов', '2000-11-30');

-- Тестовые фильмы
INSERT INTO films (name, description, release_date, duration, mpa_rating) VALUES
    ('Начало', 'Фильм о сновидениях.', '2010-07-16', 148, 'PG-13'),
    ('Крестный отец', 'Эпическая история.', '1972-03-24', 175, 'R'),
    ('Побег из Шоушенка', 'История о дружбе.', '1994-09-22', 142, 'R'),
    ('Интерстеллар', 'Путешествие через червоточину.', '2014-10-26', 169, 'PG-13'),
    ('Форрест Гамп', 'Жизнь человека.', '1994-07-06', 142, 'PG-13');

-- Тестовые связи фильмов с жанрами
INSERT INTO film_genres (film_id, genre_id) VALUES
    (1, 2), (1, 4), (2, 2), (2, 6), (3, 2),
    (4, 2), (5, 1), (5, 2);

-- Тестовые дружеские связи
INSERT INTO friendships (user_id, friend_id, status) VALUES
    (1, 2, 'confirmed'),
    (1, 3, 'confirmed'),
    (2, 3, 'confirmed'),
    (2, 4, 'pending'),
    (3, 4, 'confirmed');