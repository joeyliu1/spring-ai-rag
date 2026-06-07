package com.lss.springairag.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

/**
 * 
 * @TableName sensitive_category
 */
@TableName(value ="sensitive_category")
@Data
public class SensitiveCategory {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 分类名
     */
    private String categoryName;

    /**
     * 创建时间
     */
    private LocalDate createdTime;

    /**
     * 更新时间
     */
    private LocalDate updateTime;

    /**
     * 状态
     */
    private String status;
}
