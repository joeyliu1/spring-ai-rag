package com.lss.springairag.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class KnowledgeChunkVO {

    private Long id;

    private Integer fileId;

    private String documentId;

    private String source;

    private Integer chunkIndex;

    private Integer chunkCount;

    private Integer chunkSize;

    private String preview;

    private String content;

    private Map<String, Object> metadata;
}
