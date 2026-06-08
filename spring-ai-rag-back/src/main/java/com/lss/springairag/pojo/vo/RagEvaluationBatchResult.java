package com.lss.springairag.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RagEvaluationBatchResult {

    private int caseCount;

    private int strategyCount;

    private List<StrategyResult> strategies;

    @Data
    @Builder
    public static class StrategyResult {

        private int topK;

        private double similarityThreshold;

        private int totalCases;

        private int passedCases;

        private double passRate;

        private double averageRetrievedCount;

        private double averageScore;

        private double averageSourceHitRate;

        private double averageKeywordHitRate;

        private double recallAtK;

        private double meanReciprocalRank;

        private List<RagEvaluationResult> results;
    }
}
