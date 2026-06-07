ALTER TABLE `ali_oss_file`
  ADD COLUMN `owner_user_id` BIGINT COMMENT '文件所有者用户ID' AFTER `vector_id`,
  ADD COLUMN `team_id` BIGINT COMMENT '团队ID' AFTER `owner_user_id`,
  ADD KEY `idx_ali_oss_file_owner` (`owner_user_id`),
  ADD KEY `idx_ali_oss_file_team` (`team_id`);

-- 历史知识库默认归属管理员，避免启用隔离后已有文件不可见。
UPDATE `ali_oss_file` SET `owner_user_id` = 1 WHERE `owner_user_id` IS NULL;
