-- MySQL 5.7 版本适配

-- ----------------------------
-- Table structure for vector_store
-- ----------------------------
DROP TABLE IF EXISTS `vector_store`;
CREATE TABLE `vector_store` (
                                `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                `content` TEXT,
                                `metadata` JSON,
                                `embedding` JSON COMMENT '向量数据，存储为JSON数组，原PostgreSQL为vector(1536)',
                                PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ----------------------------
-- Table structure for tb_user
-- ----------------------------
DROP TABLE IF EXISTS `tb_user`;
CREATE TABLE `tb_user` (
                           `id` INT NOT NULL AUTO_INCREMENT,
                           `name` VARCHAR(255) NOT NULL COMMENT '姓名',
                           `user_name` VARCHAR(255) NOT NULL COMMENT '用户名',
                           `password` VARCHAR(255) NOT NULL COMMENT '密码',
                           `phone` VARCHAR(255) NOT NULL COMMENT '手机号',
                           `sex` VARCHAR(255) NOT NULL COMMENT '性别',
                           `id_number` VARCHAR(255) NOT NULL COMMENT '身份证号',
                           `status` INT NOT NULL DEFAULT 1 COMMENT '状态 0：禁用 1：启用',
                           `create_time` DATE COMMENT '创建时间',
                           `update_time` DATE COMMENT '更新时间',
                           `create_user` BIGINT COMMENT '创建人',
                           `update_user` BIGINT COMMENT '修改人',
                           PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=666498 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ----------------------------
-- Records of tb_user
-- ----------------------------
INSERT INTO `tb_user` (`id`, `name`, `user_name`, `password`, `phone`, `sex`, `id_number`, `status`, `create_time`, `update_time`, `create_user`, `update_user`)
VALUES (666497, '管理员', 'admin', '21232f297a57a5a743894a0e4a801fc3', '13800138000', '男', '11010519491231002X', 1, '2025-03-03', '2025-03-03', NULL, NULL);


-- ----------------------------
-- Table structure for ali_oss_file
-- ----------------------------
DROP TABLE IF EXISTS `ali_oss_file`;
CREATE TABLE `ali_oss_file` (
                                `id` BIGINT NOT NULL AUTO_INCREMENT,
                                `file_name` VARCHAR(255) COMMENT '文件名',
                                `url` VARCHAR(500) COMMENT '链接地址',
                                `vector_id` TEXT COMMENT '该文件分割出的多段向量文本ID',
                                `create_time` TIMESTAMP NULL DEFAULT NULL COMMENT '创建时间',
                                `update_time` TIMESTAMP NULL DEFAULT NULL COMMENT '更新时间',
                                PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='阿里云OSS文件表';


-- ----------------------------
-- Table structure for knowledge_chunk
-- ----------------------------
DROP TABLE IF EXISTS `knowledge_chunk`;
CREATE TABLE `knowledge_chunk` (
                                     `id` BIGINT NOT NULL AUTO_INCREMENT,
                                     `file_id` BIGINT NOT NULL COMMENT '知识库文件ID',
                                     `document_id` VARCHAR(255) COMMENT '向量文档ID',
                                     `source` VARCHAR(255) COMMENT '来源文件名',
                                     `chunk_index` INT COMMENT '分块序号',
                                     `chunk_count` INT COMMENT '文件分块总数',
                                     `chunk_size` INT COMMENT '分块字符数',
                                     `content` MEDIUMTEXT COMMENT '分块内容',
                                     `metadata` JSON COMMENT '分块元数据',
                                     `create_time` TIMESTAMP NULL DEFAULT NULL COMMENT '创建时间',
                                     `update_time` TIMESTAMP NULL DEFAULT NULL COMMENT '更新时间',
                                     PRIMARY KEY (`id`),
                                     INDEX `idx_knowledge_chunk_file_id` (`file_id`),
                                     INDEX `idx_knowledge_chunk_document_id` (`document_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库分块表';


-- ----------------------------
-- Table structure for log_info
-- ----------------------------
DROP TABLE IF EXISTS `log_info`;
CREATE TABLE `log_info` (
                            `id` BIGINT NOT NULL AUTO_INCREMENT,
                            `method_name` VARCHAR(255) COMMENT '方法名',
                            `class_name` VARCHAR(255) COMMENT '类目',
                            `request_time` DATE COMMENT '请求时间戳',
                            `request_params` TEXT COMMENT '请求参数',
                            `response` TEXT COMMENT '响应结果',
                            PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='日志信息表';


-- ----------------------------
-- Table structure for sensitive_word
-- ----------------------------
DROP TABLE IF EXISTS `sensitive_word`;
CREATE TABLE `sensitive_word` (
                                  `id` INT NOT NULL AUTO_INCREMENT,
                                  `word` VARCHAR(255) COMMENT '敏感词内容',
                                  `category` VARCHAR(255) COMMENT '敏感词类别',
                                  `status` VARCHAR(50) COMMENT '敏感词状态',
                                  `created_at` VARCHAR(50) COMMENT '创建时间戳',
                                  `updated_at` VARCHAR(50) COMMENT '更新时间戳',
                                  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='敏感词表';


-- ----------------------------
-- Table structure for word_frequency
-- ----------------------------
DROP TABLE IF EXISTS `word_frequency`;
CREATE TABLE `word_frequency` (
                                  `id` INT NOT NULL AUTO_INCREMENT,
                                  `word` VARCHAR(255) COMMENT '分词',
                                  `count_num` INT COMMENT '出现频次',
                                  `business_type` VARCHAR(255) COMMENT '业务类型',
                                  `create_time` DATE COMMENT '创建时间',
                                  `update_time` DATE COMMENT '更新时间',
                                  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='词频统计表';


-- ----------------------------
-- Table structure for sensitive_category
-- ----------------------------
DROP TABLE IF EXISTS `sensitive_category`;
CREATE TABLE `sensitive_category` (
                                      `id` INT NOT NULL AUTO_INCREMENT,
                                      `category_name` VARCHAR(255) COMMENT '分类名',
                                      `created_time` DATE COMMENT '创建时间',
                                      `update_time` DATE COMMENT '更新时间',
                                      `status` VARCHAR(50) COMMENT '状态',
                                      PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='敏感词分类表';


-- ----------------------------
-- Table structure for sensitive_audit_log
-- ----------------------------
DROP TABLE IF EXISTS `sensitive_audit_log`;
CREATE TABLE `sensitive_audit_log` (
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
