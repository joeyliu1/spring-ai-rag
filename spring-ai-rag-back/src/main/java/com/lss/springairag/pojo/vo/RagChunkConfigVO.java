package com.lss.springairag.pojo.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RagChunkConfigVO {

    private int chunkSize;

    private int overlapSize;

    private int minChunkSize;

    private int maxChunks;

    private int minAllowedChunkSize;

    private int maxAllowedChunkSize;

    private int maxAllowedOverlapSize;

    private int maxAllowedChunks;
}
