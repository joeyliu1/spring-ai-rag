package com.lss.springairag.pojo.vo;

import lombok.Data;

@Data
public class SensitiveWordVO {
    private Long id;
    private String word;
    private Long categoryId;
    private String categoryName;
    private String status;
    private String createdAt;
    private String updatedAt;
}
