CREATE DATABASE IF NOT EXISTS campus_life_ai
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'campus_life_ai_user'@'%' IDENTIFIED BY 'ChangeMe_2026!';

GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, DROP
    ON campus_life_ai.* TO 'campus_life_ai_user'@'%';

FLUSH PRIVILEGES;

USE campus_life_ai;

SELECT DATABASE() AS current_database;
