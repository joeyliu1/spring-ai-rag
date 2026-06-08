package com.lss.springairag.pojo.dto;

import lombok.Data;

import java.util.List;

@Data
public class RagEvaluationBatchRequest {

    private List<RagEvaluationRequest> cases;

    private List<Integer> topKValues;

    private List<Double> similarityThresholds;
}
