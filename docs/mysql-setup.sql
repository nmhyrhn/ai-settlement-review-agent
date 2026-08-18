create database if not exists settlement_review
    character set utf8mb4 collate utf8mb4_unicode_ci;

create user if not exists 'settlement'@'localhost' identified by 'settlement';
grant all privileges on settlement_review.* to 'settlement'@'localhost';
flush privileges;
