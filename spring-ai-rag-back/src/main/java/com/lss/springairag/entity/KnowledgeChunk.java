package com.lss.springairag.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName(value = "knowledge_chunk")
public class KnowledgeChunk {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Integer fileId;

    private String documentId;

    private String source;

    private Integer chunkIndex;

    private Integer chunkCount;

    private Integer chunkSize;

    private String content;

    private String metadata;

    private Date createTime;

    private Date updateTime;
}
