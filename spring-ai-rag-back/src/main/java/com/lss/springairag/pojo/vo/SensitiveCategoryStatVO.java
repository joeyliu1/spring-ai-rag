package com.lss.springairag.pojo.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SensitiveCategoryStatVO {

    private String category;

    private String riskLevel;

    private Long hitCount;

    private Long blockCount;
}
