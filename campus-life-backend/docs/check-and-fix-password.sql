-- 检查和修复用户密码的 SQL 脚本
-- 用于修复演示账号 13800138000 的密码问题

-- 1. 检查当前用户密码格式
SELECT 
    id, 
    phone, 
    username, 
    password_hash,
    LENGTH(password_hash) as hash_length,
    CASE 
        WHEN password_hash IS NULL OR password_hash = '' THEN '空密码'
        WHEN password_hash LIKE '$2a$%' OR password_hash LIKE '$2b$%' OR password_hash LIKE '$2y$%' 
        THEN 'BCrypt格式 ✓' 
        ELSE '非BCrypt格式 ✗ (需要修复)' 
    END as password_format
FROM `user` 
WHERE phone = '13800138000';

-- 2. 如果密码不是 BCrypt 格式，执行以下更新
-- 将密码 "123456" 转换为 BCrypt 格式
-- BCrypt 哈希值：$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
UPDATE `user` 
SET 
    password_hash = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    update_time = NOW()
WHERE phone = '13800138000'
  AND (password_hash IS NULL 
       OR password_hash = '' 
       OR password_hash NOT LIKE '$2a$%' 
       AND password_hash NOT LIKE '$2b$%' 
       AND password_hash NOT LIKE '$2y$%');

-- 3. 验证修复结果
SELECT 
    id, 
    phone, 
    username, 
    password_hash,
    CASE 
        WHEN password_hash LIKE '$2a$%' OR password_hash LIKE '$2b$%' OR password_hash LIKE '$2y$%' 
        THEN 'BCrypt格式 ✓' 
        ELSE '非BCrypt格式 ✗' 
    END as password_format
FROM `user` 
WHERE phone = '13800138000';

-- 4. 检查所有可能有问题的密码（非 BCrypt 格式）
SELECT 
    id, 
    phone, 
    username, 
    password_hash,
    CASE 
        WHEN password_hash IS NULL OR password_hash = '' THEN '空密码'
        WHEN password_hash LIKE '$2a$%' OR password_hash LIKE '$2b$%' OR password_hash LIKE '$2y$%' 
        THEN 'BCrypt格式 ✓' 
        ELSE '非BCrypt格式 ✗' 
    END as password_format
FROM `user`
WHERE password_hash IS NOT NULL 
  AND password_hash != ''
  AND password_hash NOT LIKE '$2a$%'
  AND password_hash NOT LIKE '$2b$%'
  AND password_hash NOT LIKE '$2y$%'
ORDER BY id;

