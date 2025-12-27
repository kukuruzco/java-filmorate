DELETE FROM likes;
DELETE FROM film_genres;
DELETE FROM friendships;
DELETE FROM films;
DELETE FROM users;
DELETE FROM genres;
DELETE FROM mpa_ratings;

INSERT INTO mpa_ratings (id, name) VALUES
(1, 'G'),
(2, 'PG'),
(3, 'PG-13'),
(4, 'R'),
(5, 'NC-17');

INSERT INTO genres (id, name) VALUES
(1, 'Комедия'),
(2, 'Драма'),
(3, 'Мультфильм'),
(4, 'Триллер'),
(5, 'Документальный'),
(6, 'Боевик');

INSERT INTO films (name, description, release_date, duration, mpa_rating) VALUES
('Начало', 'Фильм о сновидениях.', '2010-07-16', 148, 3),
('Крестный отец', 'Эпическая история.', '1972-03-24', 175, 4),
('Побег из Шоушенка', 'История о дружбе.', '1994-09-22', 142, 4),
('Интерстеллар', 'Путешествие через червоточину.', '2014-10-26', 169, 3),
('Форрест Гамп', 'Жизнь человека.', '1994-07-06', 142, 3);

INSERT INTO users (email, login, name, birthday) VALUES
('user1@example.com', 'user1', 'Анна Иванова', '1990-05-15'),
('user2@example.com', 'user2', 'Петр Петров', '1992-08-22'),
('user3@example.com', 'user3', 'Мария Сидорова', '1994-03-10'),
('user4@example.com', 'user4', 'Иван Козлов', '1991-11-05');

INSERT INTO friendships (user_id, friend_id, status) VALUES
-- Пользователь 1 дружит с пользователями 2 и 3 (подтвержденная дружба)
(1, 2, 'confirmed'),
(2, 1, 'confirmed'),  -- взаимная связь
(1, 3, 'confirmed'),
(3, 1, 'confirmed'),  -- взаимная связь

-- Пользователь 2 дружит с пользователем 3 (подтвержденная дружба)
(2, 3, 'confirmed'),
(3, 2, 'confirmed'),  -- взаимная связь

-- Пользователь 2 отправил запрос пользователю 4 (неподтвержденный)
(2, 4, 'unconfirmed'),

-- Пользователь 3 дружит с пользователем 4 (подтвержденная дружба)
(3, 4, 'confirmed'),
(4, 3, 'confirmed');