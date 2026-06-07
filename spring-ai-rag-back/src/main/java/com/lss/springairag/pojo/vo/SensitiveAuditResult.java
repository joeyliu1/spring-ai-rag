package com.lss.springairag.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SensitiveAuditResult {

    private boolean hit;

    private boolean blocked;

    private String riskLevel;

    private String blockMessage;

    private List<HitWord> hitWords;

    @Data
    @Builder
    public static class HitWord {
        private String word;
        private String category;
        private String riskLevel;
    }
}
