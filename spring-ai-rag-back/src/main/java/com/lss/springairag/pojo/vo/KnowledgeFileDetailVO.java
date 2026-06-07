package com.lss.springairag.pojo.vo;

import com.lss.springairag.entity.AliOssFile;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class KnowledgeFileDetailVO {

    private AliOssFile file;

    private int vectorCount;

    private int chunkCount;

    private List<String> vectorIds;
}
