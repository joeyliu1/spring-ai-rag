package com.lss.springairag.controller;


import com.alibaba.fastjson2.JSON;
import com.lss.springairag.common.ApplicationConstant;
import com.lss.springairag.common.BaseResponse;
import com.lss.springairag.common.ErrorCode;
import com.lss.springairag.common.ResultUtils;
import com.lss.springairag.config.RagChunkProperties;
import com.lss.springairag.context.BaseContext;
import com.lss.springairag.entity.AliOssFile;
import com.lss.springairag.pojo.dto.QueryFileDTO;
import com.lss.springairag.pojo.vo.KnowledgeUploadResultVO;
import com.lss.springairag.pojo.vo.RagChunkConfigVO;
import com.lss.springairag.rag.RecursiveChunkSplitter;
import com.lss.springairag.service.AliOssFileService;
import com.lss.springairag.utils.AliOssUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Tag(name = "KnowledgeController", description = "知识库管理接口")
@Slf4j
@RestController
@RequestMapping(ApplicationConstant.API_VERSION + "/knowledge")
public class KnowledgeController {

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private AliOssUtil aliOssUtil;

    @Autowired
    private RagChunkProperties ragChunkProperties;




    @Autowired
    private AliOssFileService aliOssFileService;
    
    /**
     * 上传附件接口
     *
     *  1. 提供不同的分片策略
     *  2. 分片后的预览
     *
     *  text-to-sql:
     *  上传资料--->（用户选择是否聚合）----> tosql---->创建表、插入数据
     * @param
     * @return
     * @throws IOException
     */

    @Operation(summary = "upload", description = "上传附件接口")
    @PostMapping(value = "file/upload", headers = "content-type=multipart/form-data")
    public BaseResponse upload(@RequestParam("file") List<MultipartFile> files,
                               @RequestParam(required = false) Integer chunkSize,
                               @RequestParam(required = false) Integer overlapSize,
                               @RequestParam(required = false) Integer minChunkSize,
                               @RequestParam(required = false) Integer maxChunks) {
        if (files.isEmpty()) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR, "请上传文件");
        }
        RagChunkProperties chunkOptions = buildChunkOptions(chunkSize, overlapSize, minChunkSize, maxChunks);
        RecursiveChunkSplitter chunkSplitter = new RecursiveChunkSplitter(chunkOptions);
        List<KnowledgeUploadResultVO.UploadedFile> uploadedFiles = new ArrayList<>();
        int totalChunkCount = 0;
        Long userId = BaseContext.getCurrentId();

        // 上传文件
        for (MultipartFile file : files) {

            // 上传OSS

            try {
                // 原文件名
                String originalFilename = file.getOriginalFilename();
                // 文件后缀
                String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                // 随机文件名（OSS)
                String objectName = UUID.randomUUID() + extension;
                String url = aliOssUtil.upload(file.getBytes(), objectName);

                // 向量化
                // 1. 读取文件 txt pdf docx doc
                Resource resource = file.getResource();
                TikaDocumentReader reader = new TikaDocumentReader(resource);
                List<Document> documents = reader.read();



                // 2. 按标题、段落、句子优先递归分块，并保留相邻块重叠上下文
                List<Document> splitDocuments = chunkSplitter.split(documents, originalFilename);
                splitDocuments.forEach(document -> document.getMetadata().put("owner_user_id", userId));
                // 3. 向量化
                // 4. 保存向量 自动调用向量模型向量化方法
                vectorStore.add(splitDocuments);

                // 持久化到数据库
                long currMillis = System.currentTimeMillis();
                AliOssFile aliOssFile = AliOssFile.builder()
                        .fileName(originalFilename)
                        .vectorId(JSON.toJSONString(splitDocuments.stream().map(Document::getId).collect(Collectors.toList())))
                        .url(url)
                        .ownerUserId(userId)
                        .createTime(new Date(currMillis))
                        .updateTime(new Date(currMillis))
                        .build();
                aliOssFileService.save(aliOssFile);
                aliOssFileService.saveChunks(aliOssFile.getId(), splitDocuments);
                totalChunkCount += splitDocuments.size();
                uploadedFiles.add(KnowledgeUploadResultVO.UploadedFile.builder()
                        .fileId(aliOssFile.getId())
                        .fileName(originalFilename)
                        .url(url)
                        .chunkCount(splitDocuments.size())
                        .vectorCount(splitDocuments.size())
                        .build());

            }
            catch (IOException e) {
                log.error("上传文件失败", e);
                return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "上传文件失败");
            }
            catch (Exception e) {
                log.error("上传文件失败", e);
                return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "向量化失败");
            }
        }
        return ResultUtils.success(KnowledgeUploadResultVO.builder()
                .chunkConfig(toChunkConfigVO(chunkOptions))
                .fileCount(uploadedFiles.size())
                .totalChunkCount(totalChunkCount)
                .files(uploadedFiles)
                .build());
    }


    @Operation(summary = "contents",description = "文件查询")
    @GetMapping("/contents")
    public BaseResponse queryFiles(QueryFileDTO request){
        if(request.getPage() == null || request.getPageSize() == null){
            return ResultUtils.error(ErrorCode.PARAMS_ERROR,"page 或 pageSize为空");
        }
        return aliOssFileService.queryPage(request);
    }

    @Operation(summary = "chunkConfig", description = "查询当前默认分块策略配置")
    @GetMapping("/chunk-config")
    public BaseResponse queryChunkConfig() {
        return ResultUtils.success(toChunkConfigVO(ragChunkProperties));
    }

    @Operation(summary = "detail", description = "文件详情")
    @GetMapping("/{id}")
    public BaseResponse queryFileDetail(@PathVariable Long id) {
        return aliOssFileService.queryFileDetail(id);
    }

    @Operation(summary = "chunks", description = "文件分块预览")
    @GetMapping("/{id}/chunks")
    public BaseResponse queryFileChunks(@PathVariable Long id,
                                        @RequestParam(required = false) Integer page,
                                        @RequestParam(required = false) Integer pageSize) {
        return aliOssFileService.queryFileChunks(id, page, pageSize);
    }

    @Operation(summary = "reindex", description = "重建文件向量索引")
    @PostMapping("/{id}/reindex")
    public BaseResponse rebuildFileIndex(@PathVariable Long id) {
        return aliOssFileService.rebuildIndex(id);
    }

    @Operation(summary = "delete",description = "文件删除")
    @DeleteMapping("/delete")
    public BaseResponse deleteFiles(@RequestParam List<Long> ids){
        return aliOssFileService.deleteFiles(ids);
    }


    @Operation(summary = "download",description = "文件下载")
    @GetMapping("/download")
    public BaseResponse downloadFiles(@RequestParam List<Long> ids){
        return aliOssFileService.downloadFiles(ids);
    }

    private RagChunkProperties buildChunkOptions(Integer chunkSize,
                                                 Integer overlapSize,
                                                 Integer minChunkSize,
                                                 Integer maxChunks) {
        RagChunkProperties options = new RagChunkProperties();
        int effectiveChunkSize = normalizeChunkSize(chunkSize == null ? ragChunkProperties.getChunkSize() : chunkSize);
        int effectiveOverlapSize = normalizeOverlapSize(
                overlapSize == null ? ragChunkProperties.getOverlapSize() : overlapSize,
                effectiveChunkSize);
        options.setChunkSize(effectiveChunkSize);
        options.setOverlapSize(effectiveOverlapSize);
        options.setMinChunkSize(normalizeMinChunkSize(
                minChunkSize == null ? ragChunkProperties.getMinChunkSize() : minChunkSize,
                effectiveChunkSize));
        options.setMaxChunks(normalizeMaxChunks(maxChunks == null ? ragChunkProperties.getMaxChunks() : maxChunks));
        return options;
    }

    private RagChunkConfigVO toChunkConfigVO(RagChunkProperties properties) {
        int chunkSize = normalizeChunkSize(properties.getChunkSize());
        return RagChunkConfigVO.builder()
                .chunkSize(chunkSize)
                .overlapSize(normalizeOverlapSize(properties.getOverlapSize(), chunkSize))
                .minChunkSize(normalizeMinChunkSize(properties.getMinChunkSize(), chunkSize))
                .maxChunks(normalizeMaxChunks(properties.getMaxChunks()))
                .minAllowedChunkSize(200)
                .maxAllowedChunkSize(4000)
                .maxAllowedOverlapSize(chunkSize / 2)
                .maxAllowedChunks(20000)
                .build();
    }

    private int normalizeChunkSize(int chunkSize) {
        return Math.max(200, Math.min(chunkSize, 4000));
    }

    private int normalizeOverlapSize(int overlapSize, int chunkSize) {
        return Math.max(0, Math.min(overlapSize, chunkSize / 2));
    }

    private int normalizeMinChunkSize(int minChunkSize, int chunkSize) {
        return Math.max(1, Math.min(minChunkSize, chunkSize));
    }

    private int normalizeMaxChunks(int maxChunks) {
        return Math.max(1, Math.min(maxChunks, 20000));
    }




}
