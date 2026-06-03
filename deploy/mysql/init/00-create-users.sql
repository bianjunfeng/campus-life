CREATE USER IF NOT EXISTS 'campus_user'@'%' IDENTIFIED BY 'campus_password';
CREATE USER IF NOT EXISTS 'campus_life_ai_user'@'%' IDENTIFIED BY 'campus_ai_password';

GRANT ALL PRIVILEGES ON `campus_life`.* TO 'campus_user'@'%';
GRANT ALL PRIVILEGES ON `campus_life_ai`.* TO 'campus_life_ai_user'@'%';

FLUSH PRIVILEGES;
