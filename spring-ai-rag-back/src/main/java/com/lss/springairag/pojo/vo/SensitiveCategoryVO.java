package com.lss.springairag.pojo.vo;

import lombok.Data;

import java.time.LocalDate;

@Data
public class SensitiveCategoryVO {
    private Long id;
    private String categoryName;
    private LocalDate createdTime;
    private LocalDate updateTime;
    private String status;
    private Long wordCount;
}
