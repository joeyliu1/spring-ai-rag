package com.lss.springairag.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class KnowledgePreviewResultVO {

    private RagChunkConfigVO chunkConfig;

    private int fileCount;

    private int totalChunkCount;

    private List<PreviewedFile> files;

    @Data
    @Builder
    public static class PreviewedFile {

        private String fileName;

        private int chunkCount;

        private List<KnowledgeChunkVO> chunks;
    }
}
