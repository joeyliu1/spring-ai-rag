CREATE TABLE IF NOT EXISTS `sensitive_audit_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT COMMENT '用户ID',
  `direction` VARCHAR(20) COMMENT '审核方向：INPUT/OUTPUT',
  `scene` VARCHAR(50) COMMENT '业务场景',
  `word` VARCHAR(255) COMMENT '命中的敏感词',
  `category` VARCHAR(255) COMMENT '敏感词分类',
  `risk_level` VARCHAR(20) COMMENT '风险等级：LOW/MEDIUM/HIGH',
  `action` VARCHAR(20) COMMENT '处理动作：PASS/BLOCK',
  `content_preview` TEXT COMMENT '命中内容预览',
  `create_time` DATETIME COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_direction` (`direction`),
  KEY `idx_category` (`category`),
  KEY `idx_risk_level` (`risk_level`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='敏感词命中审核日志表';
