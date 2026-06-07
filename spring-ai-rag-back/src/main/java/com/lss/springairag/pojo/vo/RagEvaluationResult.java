package com.lss.springairag.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class RagEvaluationResult {

    private String question;

    private int topK;

    private double similarityThreshold;

    private int retrievedCount;

    private int sourceHitCount;

    private int keywordHitCount;

    private double sourceHitRate;

    private double keywordHitRate;

    private boolean passed;

    private List<String> expectedSources;

    private List<String> expectedKeywords;

    private List<RetrievedChunk> retrievedChunks;

    @Data
    @Builder
    public static class RetrievedChunk {

        private int rank;

        private String id;

        private Double score;

        private String source;

        private Integer chunkIndex;

        private Integer chunkCount;

        private String preview;

        private boolean sourceMatched;

        private List<String> matchedKeywords;

        private Map<String, Object> metadata;
    }
}
