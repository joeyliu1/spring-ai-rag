package com.lss.springairag.pojo.dto;

import lombok.Data;

import java.util.List;

@Data
public class RagEvaluationRequest {

    /**
     * Question used to test vector retrieval.
     */
    private String question;

    /**
     * Optional source file filter. When present, retrieval only searches these
     * files.
     */
    private List<String> sources;

    /**
     * Expected source files for hit-rate calculation.
     */
    private List<String> expectedSources;

    /**
     * Expected answer keywords for rough retrieval coverage checks.
     */
    private List<String> expectedKeywords;

    private Integer topK;

    private Double similarityThreshold;
}
