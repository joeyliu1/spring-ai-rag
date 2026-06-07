-- Reset user ids to normalized BIGINT auto-increment values.
-- Admin user is fixed to id = 1. Other users are renumbered from 2.

ALTER TABLE `tb_user`
  MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT;

CREATE TEMPORARY TABLE `tmp_user_id_mapping` (
  `old_id` BIGINT NOT NULL PRIMARY KEY,
  `new_id` BIGINT NOT NULL UNIQUE
);

SET @next_user_id := 1;

INSERT INTO `tmp_user_id_mapping` (`old_id`, `new_id`)
SELECT
  `id` AS `old_id`,
  CASE
    WHEN `user_name` = 'admin' THEN 1
    ELSE (@next_user_id := @next_user_id + 1)
  END AS `new_id`
FROM `tb_user`
ORDER BY CASE WHEN `user_name` = 'admin' THEN 0 ELSE 1 END, `id`;

UPDATE `tb_user` u
JOIN `tmp_user_id_mapping` m ON u.`create_user` = m.`old_id`
SET u.`create_user` = m.`new_id`;

UPDATE `tb_user` u
JOIN `tmp_user_id_mapping` m ON u.`update_user` = m.`old_id`
SET u.`update_user` = m.`new_id`;

UPDATE `ali_oss_file` f
JOIN `tmp_user_id_mapping` m ON f.`owner_user_id` = m.`old_id`
SET f.`owner_user_id` = m.`new_id`;

UPDATE `ali_oss_file`
SET `owner_user_id` = 1
WHERE `owner_user_id` IS NULL;

UPDATE `sensitive_audit_log` l
JOIN `tmp_user_id_mapping` m ON l.`user_id` = m.`old_id`
SET l.`user_id` = m.`new_id`;

-- Two-phase primary-key rewrite avoids collisions when an existing user already has a target id.
UPDATE `tb_user` u
JOIN `tmp_user_id_mapping` m ON u.`id` = m.`old_id`
SET u.`id` = -9000000000000000000 + m.`new_id`;

UPDATE `tb_user` u
JOIN `tmp_user_id_mapping` m ON u.`id` = -9000000000000000000 + m.`new_id`
SET u.`id` = m.`new_id`;

SET @next_auto_increment := (SELECT COALESCE(MAX(`id`), 0) + 1 FROM `tb_user`);
SET @reset_auto_increment_sql := CONCAT('ALTER TABLE `tb_user` AUTO_INCREMENT = ', @next_auto_increment);
PREPARE reset_auto_increment_stmt FROM @reset_auto_increment_sql;
EXECUTE reset_auto_increment_stmt;
DEALLOCATE PREPARE reset_auto_increment_stmt;

DROP TEMPORARY TABLE `tmp_user_id_mapping`;
