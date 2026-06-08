-- 将 sensitive_word 从 category 字符串迁移为 category_id 主关联。
-- 执行前建议先备份 lss_rag 数据库。

INSERT INTO sensitive_category (category_name, created_time, update_time, status)
SELECT DISTINCT sw.category, CURDATE(), CURDATE(), '1'
FROM sensitive_word sw
LEFT JOIN sensitive_category sc ON sc.category_name = sw.category
WHERE sw.category IS NOT NULL
  AND sw.category <> ''
  AND sc.id IS NULL;

INSERT INTO sensitive_category (category_name, created_time, update_time, status)
SELECT '未分类', CURDATE(), CURDATE(), '1'
WHERE NOT EXISTS (
    SELECT 1 FROM sensitive_category WHERE category_name = '未分类'
);

ALTER TABLE sensitive_word
    ADD COLUMN category_id BIGINT NULL COMMENT '敏感词分类ID' AFTER word;

UPDATE sensitive_word sw
JOIN sensitive_category sc ON sc.category_name = sw.category
SET sw.category_id = sc.id
WHERE sw.category_id IS NULL
  AND sw.category IS NOT NULL
  AND sw.category <> '';

UPDATE sensitive_word sw
JOIN sensitive_category sc ON sc.category_name = '未分类'
SET sw.category_id = sc.id
WHERE sw.category_id IS NULL;

ALTER TABLE sensitive_category
    ADD UNIQUE KEY uk_sensitive_category_name (category_name);

ALTER TABLE sensitive_word
    MODIFY category_id BIGINT NOT NULL COMMENT '敏感词分类ID',
    ADD KEY idx_sensitive_word_category_id (category_id),
    ADD CONSTRAINT fk_sensitive_word_category
        FOREIGN KEY (category_id) REFERENCES sensitive_category (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    DROP COLUMN category;
