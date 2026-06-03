-- 修复 post_image 表 url 列长度问题
-- 问题：url 列可能被定义为 varchar(255)，无法存储 base64 编码的长 URL
-- 解决方案：将 url 列类型修改为 TEXT 或 LONGTEXT

-- 检查当前列类型（可选，用于诊断）
-- SHOW COLUMNS FROM post_image WHERE Field = 'url';

-- 修改 url 列为 TEXT 类型（可存储最多 65,535 字符）
-- 如果您的图片 URL 可能更长（如超大的 base64 图片），可以使用 LONGTEXT（可存储最多 4GB）
ALTER TABLE `post_image` 
MODIFY COLUMN `url` TEXT NOT NULL COMMENT '图片地址（支持 base64 或 URL）';

-- 如果需要支持更大的 URL（如超大的 base64 图片），可以使用以下语句：
-- ALTER TABLE `post_image` 
-- MODIFY COLUMN `url` LONGTEXT NOT NULL COMMENT '图片地址（支持 base64 或 URL）';

