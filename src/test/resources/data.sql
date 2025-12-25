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
('user1@test.com', 'user1', 'User One', '1990-01-01'),
('user2@test.com', 'user2', 'User Two', '1992-02-02'),
('user3@test.com', 'user3', 'User Three', '1994-03-03');