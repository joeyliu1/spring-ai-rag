package com.lss.springairag.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class KnowledgeUploadResultVO {

    private RagChunkConfigVO chunkConfig;

    private int fileCount;

    private int totalChunkCount;

    private List<UploadedFile> files;

    @Data
    @Builder
    public static class UploadedFile {

        private Integer fileId;

        private String fileName;

        private String url;

        private int chunkCount;

        private int vectorCount;
    }
}
