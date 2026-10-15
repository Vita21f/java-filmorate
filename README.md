## Схема базы данных

![Схема базы данных Filmorate](https://private-user-images.githubusercontent.com/271237423/674500179-ea64f789-1830-4f0b-b34e-40336cca3bdf.png?jwt=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJnaXRodWIuY29tIiwiYXVkIjoicmF3LmdpdGh1YnVzZXJjb250ZW50LmNvbSIsImtleSI6ImtleTUiLCJleHAiOjE3OTIwNTk5NDQsIm5iZiI6MTc5MjA1OTY0NCwicGF0aCI6Ii8yNzEyMzc0MjMvNjc0NTAwMTc5LWVhNjRmNzg5LTE4MzAtNGYwYi1iMzRlLTQwMzM2Y2NhM2JkZi5wbmc_WC1BbXotQWxnb3JpdGhtPUFXUzQtSE1BQy1TSEEyNTYmWC1BbXotQ3JlZGVudGlhbD1BS0lBVkNPRFlMU0E1M1BRSzRaQSUyRjIwMjYxMDE1JTJGdXMtZWFzdC0xJTJGczMlMkZhd3M0X3JlcXVlc3QmWC1BbXotRGF0ZT0yMDI2MTAxNVQxMDIwNDRaJlgtQW16LUV4cGlyZXM9MzAwJlgtQW16LVNpZ25hdHVyZT0zYmE1NzU2MTIxMGY2NjM2N2Q1NjJhNGZmNDQ4YmIzMjRlMzRkNTMzZDVjZTQ0YmIxZjQ2ZjVmMDU2ZjM4MzVlJlgtQW16LVNpZ25lZEhlYWRlcnM9aG9zdCZyZXNwb25zZS1jb250ZW50LXR5cGU9aW1hZ2UlMkZwbmcifQ.kDo45q8r8QrlxQejpXaCRG9JPX2Y98XutfKjluG8g88)

Исходник схемы: [docs/schema.dbml](docs/schema.dbml)

### Таблицы

- `films` — фильмы
- `users` — пользователи
- `mpa` — справочник рейтингов MPA (G, PG, PG-13, R, NC-17)
- `genres` — справочник жанров
- `likes`, `film_genres`, `friendship` — таблицы связей

### Связи

**Фильм → рейтинг MPA (многие к одному).**
У каждого фильма ровно один рейтинг, один рейтинг может быть у многих фильмов.
Связь хранится в колонке `films.mpa_id`, которая ссылается на `mpa.id`.

**Фильм ↔ жанр (многие ко многим).**
У фильма может быть несколько жанров, один жанр может быть у многих фильмов.
Связь хранится в таблице `film_genres`: одна строка — один жанр одного фильма.

**Пользователь ↔ фильм, лайки (многие ко многим).**
Пользователь может лайкнуть много фильмов, у фильма может быть много лайков от разных пользователей.
Связь хранится в таблице `likes`: одна строка — лайк одного пользователя одному фильму.
Составной первичный ключ `(film_id, user_id)` не даёт лайкнуть один фильм дважды.

**Пользователь ↔ пользователь, дружба (многие ко многим).**
Пользователь может дружить со многими пользователями, и у каждого из них, в свою очередь, много друзей.
Связь хранится в таблице `friendship`, обе колонки ссылаются на `users.id`.
Строка `(user_id, friend_id)` означает: у `user_id` в списке друзей есть `friend_id`.

Колонка `status` хранит состояние дружбы:
- `PENDING` — `user_id` отправил запрос, `friend_id` его ещё не принял;
- `CONFIRMED` — `friend_id` принял запрос, дружба подтверждена.

Отклонённый запрос удаляется из таблицы, отдельного статуса для него нет.