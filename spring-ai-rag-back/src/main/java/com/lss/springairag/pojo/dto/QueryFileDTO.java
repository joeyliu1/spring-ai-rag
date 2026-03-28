package com.lss.springairag.pojo.dto;

import lombok.Data;

@Data
public class QueryFileDTO {
    private Integer page;
    private Integer pageSize;
    private String fileName;
}
