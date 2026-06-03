-- 为学生档案表添加未审核状态
-- status 字段含义：
-- 0: 待审核/未审核
-- 1: 已通过/有效
-- 2: 已拒绝/无效

-- 更新表结构注释
ALTER TABLE `student_profile` 
MODIFY COLUMN `status` tinyint NOT NULL DEFAULT 0 COMMENT '0-待审核;1-已通过/有效;2-已拒绝/无效';

-- 将现有有效记录的状态从1保持不变（如果已经是1，则保持为1）
-- 将现有无效记录的状态从0改为2（如果status=0且不是新创建的待审核记录）
-- 注意：这里假设所有现有的status=0的记录都是无效的，需要根据实际情况调整
-- UPDATE `student_profile` SET `status` = 2 WHERE `status` = 0 AND `create_time` < NOW() - INTERVAL 1 DAY;

