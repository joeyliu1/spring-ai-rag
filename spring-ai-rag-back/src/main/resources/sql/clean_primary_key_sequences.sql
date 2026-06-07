-- Normalize existing primary-key data to compact BIGINT AUTO_INCREMENT sequences.
-- The script keeps admin user id = 1 and updates known references before rewriting ids.

-- tb_user: admin remains 1, other users start from 2.
DROP TEMPORARY TABLE IF EXISTS `tmp_tb_user_id_mapping`;
CREATE TEMPORARY TABLE `tmp_tb_user_id_mapping` (
  `old_id` BIGINT NOT NULL PRIMARY KEY,
  `new_id` BIGINT NOT NULL UNIQUE
);

SET @row_num := 1;
INSERT INTO `tmp_tb_user_id_mapping` (`old_id`, `new_id`)
SELECT
  `id`,
  CASE WHEN `user_name` = 'admin' THEN 1 ELSE (@row_num := @row_num + 1) END
FROM `tb_user`
ORDER BY CASE WHEN `user_name` = 'admin' THEN 0 ELSE 1 END, `id`;

UPDATE `tb_user` u JOIN `tmp_tb_user_id_mapping` m ON u.`create_user` = m.`old_id`
SET u.`create_user` = m.`new_id`;

UPDATE `tb_user` u JOIN `tmp_tb_user_id_mapping` m ON u.`update_user` = m.`old_id`
SET u.`update_user` = m.`new_id`;

UPDATE `ali_oss_file` f JOIN `tmp_tb_user_id_mapping` m ON f.`owner_user_id` = m.`old_id`
SET f.`owner_user_id` = m.`new_id`;

UPDATE `sensitive_audit_log` l JOIN `tmp_tb_user_id_mapping` m ON l.`user_id` = m.`old_id`
SET l.`user_id` = m.`new_id`;

UPDATE `tb_user` u JOIN `tmp_tb_user_id_mapping` m ON u.`id` = m.`old_id`
SET u.`id` = -9000000000000000000 + m.`new_id`;

UPDATE `tb_user` u JOIN `tmp_tb_user_id_mapping` m ON u.`id` = -9000000000000000000 + m.`new_id`
SET u.`id` = m.`new_id`;

SET @reset_sql := CONCAT('ALTER TABLE `tb_user` AUTO_INCREMENT = ', (SELECT COALESCE(MAX(`id`), 0) + 1 FROM `tb_user`));
PREPARE reset_stmt FROM @reset_sql;
EXECUTE reset_stmt;
DEALLOCATE PREPARE reset_stmt;
DROP TEMPORARY TABLE IF EXISTS `tmp_tb_user_id_mapping`;

-- ali_oss_file: update knowledge_chunk.file_id before rewriting file ids.
DROP TEMPORARY TABLE IF EXISTS `tmp_ali_oss_file_id_mapping`;
CREATE TEMPORARY TABLE `tmp_ali_oss_file_id_mapping` (
  `old_id` BIGINT NOT NULL PRIMARY KEY,
  `new_id` BIGINT NOT NULL UNIQUE
);

SET @row_num := 0;
INSERT INTO `tmp_ali_oss_file_id_mapping` (`old_id`, `new_id`)
SELECT `id`, (@row_num := @row_num + 1)
FROM `ali_oss_file`
ORDER BY `id`;

UPDATE `knowledge_chunk` c JOIN `tmp_ali_oss_file_id_mapping` m ON c.`file_id` = m.`old_id`
SET c.`file_id` = m.`new_id`;

UPDATE `ali_oss_file` f JOIN `tmp_ali_oss_file_id_mapping` m ON f.`id` = m.`old_id`
SET f.`id` = -9000000000000000000 + m.`new_id`;

UPDATE `ali_oss_file` f JOIN `tmp_ali_oss_file_id_mapping` m ON f.`id` = -9000000000000000000 + m.`new_id`
SET f.`id` = m.`new_id`;

SET @reset_sql := CONCAT('ALTER TABLE `ali_oss_file` AUTO_INCREMENT = ', (SELECT COALESCE(MAX(`id`), 0) + 1 FROM `ali_oss_file`));
PREPARE reset_stmt FROM @reset_sql;
EXECUTE reset_stmt;
DEALLOCATE PREPARE reset_stmt;
DROP TEMPORARY TABLE IF EXISTS `tmp_ali_oss_file_id_mapping`;

-- knowledge_chunk
DROP TEMPORARY TABLE IF EXISTS `tmp_knowledge_chunk_id_mapping`;
CREATE TEMPORARY TABLE `tmp_knowledge_chunk_id_mapping` (
  `old_id` BIGINT NOT NULL PRIMARY KEY,
  `new_id` BIGINT NOT NULL UNIQUE
);

SET @row_num := 0;
INSERT INTO `tmp_knowledge_chunk_id_mapping` (`old_id`, `new_id`)
SELECT `id`, (@row_num := @row_num + 1)
FROM `knowledge_chunk`
ORDER BY `id`;

UPDATE `knowledge_chunk` t JOIN `tmp_knowledge_chunk_id_mapping` m ON t.`id` = m.`old_id`
SET t.`id` = -9000000000000000000 + m.`new_id`;

UPDATE `knowledge_chunk` t JOIN `tmp_knowledge_chunk_id_mapping` m ON t.`id` = -9000000000000000000 + m.`new_id`
SET t.`id` = m.`new_id`;

SET @reset_sql := CONCAT('ALTER TABLE `knowledge_chunk` AUTO_INCREMENT = ', (SELECT COALESCE(MAX(`id`), 0) + 1 FROM `knowledge_chunk`));
PREPARE reset_stmt FROM @reset_sql;
EXECUTE reset_stmt;
DEALLOCATE PREPARE reset_stmt;
DROP TEMPORARY TABLE IF EXISTS `tmp_knowledge_chunk_id_mapping`;

-- log_info
DROP TEMPORARY TABLE IF EXISTS `tmp_log_info_id_mapping`;
CREATE TEMPORARY TABLE `tmp_log_info_id_mapping` (
  `old_id` BIGINT NOT NULL PRIMARY KEY,
  `new_id` BIGINT NOT NULL UNIQUE
);

SET @row_num := 0;
INSERT INTO `tmp_log_info_id_mapping` (`old_id`, `new_id`)
SELECT `id`, (@row_num := @row_num + 1)
FROM `log_info`
ORDER BY `id`;

UPDATE `log_info` t JOIN `tmp_log_info_id_mapping` m ON t.`id` = m.`old_id`
SET t.`id` = -9000000000000000000 + m.`new_id`;

UPDATE `log_info` t JOIN `tmp_log_info_id_mapping` m ON t.`id` = -9000000000000000000 + m.`new_id`
SET t.`id` = m.`new_id`;

SET @reset_sql := CONCAT('ALTER TABLE `log_info` AUTO_INCREMENT = ', (SELECT COALESCE(MAX(`id`), 0) + 1 FROM `log_info`));
PREPARE reset_stmt FROM @reset_sql;
EXECUTE reset_stmt;
DEALLOCATE PREPARE reset_stmt;
DROP TEMPORARY TABLE IF EXISTS `tmp_log_info_id_mapping`;

-- sensitive_word
DROP TEMPORARY TABLE IF EXISTS `tmp_sensitive_word_id_mapping`;
CREATE TEMPORARY TABLE `tmp_sensitive_word_id_mapping` (
  `old_id` BIGINT NOT NULL PRIMARY KEY,
  `new_id` BIGINT NOT NULL UNIQUE
);

SET @row_num := 0;
INSERT INTO `tmp_sensitive_word_id_mapping` (`old_id`, `new_id`)
SELECT `id`, (@row_num := @row_num + 1)
FROM `sensitive_word`
ORDER BY `id`;

UPDATE `sensitive_word` t JOIN `tmp_sensitive_word_id_mapping` m ON t.`id` = m.`old_id`
SET t.`id` = -9000000000000000000 + m.`new_id`;

UPDATE `sensitive_word` t JOIN `tmp_sensitive_word_id_mapping` m ON t.`id` = -9000000000000000000 + m.`new_id`
SET t.`id` = m.`new_id`;

SET @reset_sql := CONCAT('ALTER TABLE `sensitive_word` AUTO_INCREMENT = ', (SELECT COALESCE(MAX(`id`), 0) + 1 FROM `sensitive_word`));
PREPARE reset_stmt FROM @reset_sql;
EXECUTE reset_stmt;
DEALLOCATE PREPARE reset_stmt;
DROP TEMPORARY TABLE IF EXISTS `tmp_sensitive_word_id_mapping`;

-- word_frequency
DROP TEMPORARY TABLE IF EXISTS `tmp_word_frequency_id_mapping`;
CREATE TEMPORARY TABLE `tmp_word_frequency_id_mapping` (
  `old_id` BIGINT NOT NULL PRIMARY KEY,
  `new_id` BIGINT NOT NULL UNIQUE
);

SET @row_num := 0;
INSERT INTO `tmp_word_frequency_id_mapping` (`old_id`, `new_id`)
SELECT `id`, (@row_num := @row_num + 1)
FROM `word_frequency`
ORDER BY `id`;

UPDATE `word_frequency` t JOIN `tmp_word_frequency_id_mapping` m ON t.`id` = m.`old_id`
SET t.`id` = -9000000000000000000 + m.`new_id`;

UPDATE `word_frequency` t JOIN `tmp_word_frequency_id_mapping` m ON t.`id` = -9000000000000000000 + m.`new_id`
SET t.`id` = m.`new_id`;

SET @reset_sql := CONCAT('ALTER TABLE `word_frequency` AUTO_INCREMENT = ', (SELECT COALESCE(MAX(`id`), 0) + 1 FROM `word_frequency`));
PREPARE reset_stmt FROM @reset_sql;
EXECUTE reset_stmt;
DEALLOCATE PREPARE reset_stmt;
DROP TEMPORARY TABLE IF EXISTS `tmp_word_frequency_id_mapping`;

-- sensitive_category
DROP TEMPORARY TABLE IF EXISTS `tmp_sensitive_category_id_mapping`;
CREATE TEMPORARY TABLE `tmp_sensitive_category_id_mapping` (
  `old_id` BIGINT NOT NULL PRIMARY KEY,
  `new_id` BIGINT NOT NULL UNIQUE
);

SET @row_num := 0;
INSERT INTO `tmp_sensitive_category_id_mapping` (`old_id`, `new_id`)
SELECT `id`, (@row_num := @row_num + 1)
FROM `sensitive_category`
ORDER BY `id`;

UPDATE `sensitive_category` t JOIN `tmp_sensitive_category_id_mapping` m ON t.`id` = m.`old_id`
SET t.`id` = -9000000000000000000 + m.`new_id`;

UPDATE `sensitive_category` t JOIN `tmp_sensitive_category_id_mapping` m ON t.`id` = -9000000000000000000 + m.`new_id`
SET t.`id` = m.`new_id`;

SET @reset_sql := CONCAT('ALTER TABLE `sensitive_category` AUTO_INCREMENT = ', (SELECT COALESCE(MAX(`id`), 0) + 1 FROM `sensitive_category`));
PREPARE reset_stmt FROM @reset_sql;
EXECUTE reset_stmt;
DEALLOCATE PREPARE reset_stmt;
DROP TEMPORARY TABLE IF EXISTS `tmp_sensitive_category_id_mapping`;

-- sensitive_audit_log
DROP TEMPORARY TABLE IF EXISTS `tmp_sensitive_audit_log_id_mapping`;
CREATE TEMPORARY TABLE `tmp_sensitive_audit_log_id_mapping` (
  `old_id` BIGINT NOT NULL PRIMARY KEY,
  `new_id` BIGINT NOT NULL UNIQUE
);

SET @row_num := 0;
INSERT INTO `tmp_sensitive_audit_log_id_mapping` (`old_id`, `new_id`)
SELECT `id`, (@row_num := @row_num + 1)
FROM `sensitive_audit_log`
ORDER BY `id`;

UPDATE `sensitive_audit_log` t JOIN `tmp_sensitive_audit_log_id_mapping` m ON t.`id` = m.`old_id`
SET t.`id` = -9000000000000000000 + m.`new_id`;

UPDATE `sensitive_audit_log` t JOIN `tmp_sensitive_audit_log_id_mapping` m ON t.`id` = -9000000000000000000 + m.`new_id`
SET t.`id` = m.`new_id`;

SET @reset_sql := CONCAT('ALTER TABLE `sensitive_audit_log` AUTO_INCREMENT = ', (SELECT COALESCE(MAX(`id`), 0) + 1 FROM `sensitive_audit_log`));
PREPARE reset_stmt FROM @reset_sql;
EXECUTE reset_stmt;
DEALLOCATE PREPARE reset_stmt;
DROP TEMPORARY TABLE IF EXISTS `tmp_sensitive_audit_log_id_mapping`;

-- Empty vector_store: reset counter to 1.
ALTER TABLE `vector_store` AUTO_INCREMENT = 1;
