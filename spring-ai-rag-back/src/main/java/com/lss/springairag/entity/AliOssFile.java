package com.lss.springairag.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * @TableName ali_oss_file
 */
@TableName(value ="ali_oss_file")
@Data
@Builder
public class AliOssFile {
    /**
     * 主键id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 链接地址
     */
    private String url;

    /**
     * 该文件分割出的多段向量文本ID
     */
    private String vectorId;

    /**
     * 文件所有者用户ID
     */
    private Long ownerUserId;

    /**
     * 团队ID，预留团队知识库隔离能力
     */
    private Long teamId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}
