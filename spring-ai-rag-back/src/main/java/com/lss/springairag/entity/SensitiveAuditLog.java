package com.lss.springairag.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @TableName sensitive_audit_log
 */
@TableName(value = "sensitive_audit_log")
@Data
public class SensitiveAuditLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /**
     * INPUT / OUTPUT
     */
    private String direction;

    /**
     * chat / rag_chat
     */
    private String scene;

    private String word;

    private String category;

    /**
     * LOW / MEDIUM / HIGH
     */
    private String riskLevel;

    /**
     * PASS / BLOCK
     */
    private String action;

    private String contentPreview;

    private LocalDateTime createTime;
}
