-- Replace CHANGE_ME_AI_MYSQL_PASSWORD before running.
CREATE DATABASE IF NOT EXISTS campus_life_ai_demo
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'campus_life_ai_user'@'%' IDENTIFIED BY 'CHANGE_ME_AI_MYSQL_PASSWORD';

GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, DROP
    ON campus_life_ai_demo.* TO 'campus_life_ai_user'@'%';

FLUSH PRIVILEGES;
