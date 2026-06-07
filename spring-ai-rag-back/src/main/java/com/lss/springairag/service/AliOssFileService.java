package com.lss.springairag.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lss.springairag.common.BaseResponse;
import com.lss.springairag.entity.AliOssFile;
import com.lss.springairag.pojo.dto.QueryFileDTO;
import org.springframework.ai.document.Document;

import java.util.List;

public interface AliOssFileService extends IService<AliOssFile> {

    BaseResponse queryPage(QueryFileDTO request);

    BaseResponse deleteFiles(List<Long> ids);

    BaseResponse downloadFiles(List<Long> ids);

    void saveChunks(Integer fileId, List<Document> documents);

    BaseResponse queryFileDetail(Long id);

    BaseResponse queryFileChunks(Long id, Integer page, Integer pageSize);

    BaseResponse rebuildIndex(Long id);
}
