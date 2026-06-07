package com.lss.springairag.pojo.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class WordFrequencyPageQueryDTO {
    private int page;
    private int pageSize;
    private String word;
    private String businessType;
    private Integer countNumMin;
    private LocalDate startDate;
    private LocalDate endDate;
}
