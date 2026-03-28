package com.lss.springairag.pojo.dto;

import lombok.Data;

import java.util.List;

@Data
public class RagQueryRequest {
    private List<String> sources;
    private String message;
}