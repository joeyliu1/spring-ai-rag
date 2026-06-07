package com.lss.springairag.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lss.springairag.common.BaseResponse;
import com.lss.springairag.common.ErrorCode;
import com.lss.springairag.common.ResultUtils;
import com.lss.springairag.context.BaseContext;
import com.lss.springairag.entity.AliOssFile;
import com.lss.springairag.entity.KnowledgeChunk;
import com.lss.springairag.mapper.AliOssFileMapper;
import com.lss.springairag.mapper.KnowledgeChunkMapper;
import com.lss.springairag.pojo.dto.QueryFileDTO;
import com.lss.springairag.pojo.vo.KnowledgeChunkVO;
import com.lss.springairag.pojo.vo.KnowledgeFileDetailVO;
import com.lss.springairag.service.AliOssFileService;
import com.lss.springairag.utils.AliOssUtil;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AliOssFileServiceImpl extends ServiceImpl<AliOssFileMapper, AliOssFile>
    implements AliOssFileService {

    @Autowired
    private AliOssFileMapper aliOssFileMapper;

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private AliOssUtil aliOssUtil;

    @Autowired
    private KnowledgeChunkMapper knowledgeChunkMapper;

    private static final int DEFAULT_CHUNK_PAGE_SIZE = 20;

    private static final int MAX_CHUNK_PAGE_SIZE = 100;

    private static final int PREVIEW_LENGTH = 220;


    /**
     * 查询文件
     * @param request
     * @return
     */
    @Override
    public BaseResponse queryPage(QueryFileDTO request) {
        Page<AliOssFile> page = new Page<>(request.getPage(), request.getPageSize());
        IPage<AliOssFile> fileList = aliOssFileMapper.findByFileNameContaining(page, request.getFileName(), currentUserId());
        return ResultUtils.success(fileList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse deleteFiles(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR, "请选择文件");
        }
        Long userId = currentUserId();
        List<AliOssFile> aliOssFiles = aliOssFileMapper.selectList(new LambdaQueryWrapper<AliOssFile>()
                .in(AliOssFile::getId, ids)
                .eq(AliOssFile::getOwnerUserId, userId));
        if (aliOssFiles.size() != ids.size()) {
            return ResultUtils.error(ErrorCode.NO_AUTH_ERROR, "只能删除自己的知识库文件");
        }
        List<Long> fileIds = aliOssFiles.stream().map(AliOssFile::getId).collect(Collectors.toList());
        int count = aliOssFileMapper.deleteBatchIds(fileIds);
        if (count == 0) {
            return ResultUtils.error(ErrorCode.OPERATION_ERROR, "删除失败");
        }
        for (AliOssFile aliOssFile : aliOssFiles) {
            List<String> vectorIds = parseVectorIds(aliOssFile.getVectorId());
            if (!vectorIds.isEmpty()) {
                vectorStore.delete(vectorIds);
            }
            knowledgeChunkMapper.delete(new LambdaQueryWrapper<KnowledgeChunk>()
                    .eq(KnowledgeChunk::getFileId, aliOssFile.getId()));
            aliOssUtil.deleteOss(aliOssFile.getUrl());
        }

        return ResultUtils.success("成功删除"+ count + "个文件");
    }

    @Override
    public BaseResponse downloadFiles(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR, "请选择文件");
        }
        List<AliOssFile> aliOssFiles = aliOssFileMapper.selectList(new LambdaQueryWrapper<AliOssFile>()
                .in(AliOssFile::getId, ids)
                .eq(AliOssFile::getOwnerUserId, currentUserId()));
        if (aliOssFiles.size() != ids.size()) {
            return ResultUtils.error(ErrorCode.NO_AUTH_ERROR, "只能下载自己的知识库文件");
        }
        for (AliOssFile aliOssFile : aliOssFiles){
            String url = aliOssFile.getUrl();
            String fileName = extractFileName(url);
            aliOssUtil.download(fileName);
        }
        return ResultUtils.success("下载成功");
    }

    @Override
    public void saveChunks(Long fileId, List<Document> documents) {
        if (fileId == null || CollectionUtils.isEmpty(documents)) {
            return;
        }
        Date now = new Date();
        for (Document document : documents) {
            Map<String, Object> metadata = document.getMetadata();
            KnowledgeChunk chunk = new KnowledgeChunk();
            chunk.setFileId(fileId);
            chunk.setDocumentId(document.getId());
            chunk.setSource(metadataValue(metadata, "source"));
            chunk.setChunkIndex(metadataInteger(metadata, "chunk_index"));
            chunk.setChunkCount(metadataInteger(metadata, "chunk_count"));
            chunk.setChunkSize(metadataInteger(metadata, "chunk_size"));
            chunk.setContent(document.getText());
            chunk.setMetadata(JSON.toJSONString(metadata));
            chunk.setCreateTime(now);
            chunk.setUpdateTime(now);
            knowledgeChunkMapper.insert(chunk);
        }
    }

    @Override
    public BaseResponse queryFileDetail(Long id) {
        if (id == null) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR, "文件 id 不能为空");
        }
        AliOssFile file = getOwnedFile(id);
        if (file == null) {
            return ResultUtils.error(ErrorCode.NOT_FOUND_ERROR, "文件不存在或无权访问");
        }
        Long chunkCount = knowledgeChunkMapper.selectCount(new LambdaQueryWrapper<KnowledgeChunk>()
                .eq(KnowledgeChunk::getFileId, file.getId()));
        List<String> vectorIds = parseVectorIds(file.getVectorId());
        return ResultUtils.success(KnowledgeFileDetailVO.builder()
                .file(file)
                .vectorCount(vectorIds.size())
                .chunkCount(chunkCount == null ? 0 : chunkCount.intValue())
                .vectorIds(vectorIds)
                .build());
    }

    @Override
    public BaseResponse queryFileChunks(Long id, Integer page, Integer pageSize) {
        if (id == null) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR, "文件 id 不能为空");
        }
        AliOssFile file = getOwnedFile(id);
        if (file == null) {
            return ResultUtils.error(ErrorCode.NOT_FOUND_ERROR, "文件不存在或无权访问");
        }
        int currentPage = page == null || page <= 0 ? 1 : page;
        int currentPageSize = pageSize == null || pageSize <= 0
                ? DEFAULT_CHUNK_PAGE_SIZE
                : Math.min(pageSize, MAX_CHUNK_PAGE_SIZE);
        Page<KnowledgeChunk> chunkPage = new Page<>(currentPage, currentPageSize);
        IPage<KnowledgeChunk> chunks = knowledgeChunkMapper.selectPage(chunkPage,
                new LambdaQueryWrapper<KnowledgeChunk>()
                        .eq(KnowledgeChunk::getFileId, file.getId())
                        .orderByAsc(KnowledgeChunk::getChunkIndex));
        Page<KnowledgeChunkVO> resultPage = new Page<>(chunks.getCurrent(), chunks.getSize(), chunks.getTotal());
        resultPage.setRecords(chunks.getRecords().stream()
                .map(this::toChunkVO)
                .collect(Collectors.toList()));
        return ResultUtils.success(resultPage);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse rebuildIndex(Long id) {
        if (id == null) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR, "文件 id 不能为空");
        }
        AliOssFile file = getOwnedFile(id);
        if (file == null) {
            return ResultUtils.error(ErrorCode.NOT_FOUND_ERROR, "文件不存在或无权访问");
        }
        List<KnowledgeChunk> chunks = knowledgeChunkMapper.selectList(new LambdaQueryWrapper<KnowledgeChunk>()
                .eq(KnowledgeChunk::getFileId, file.getId())
                .orderByAsc(KnowledgeChunk::getChunkIndex));
        if (chunks.isEmpty()) {
            return ResultUtils.error(ErrorCode.OPERATION_ERROR, "该文件缺少分块记录，无法重建索引");
        }

        List<String> oldVectorIds = parseVectorIds(file.getVectorId());
        if (!oldVectorIds.isEmpty()) {
            vectorStore.delete(oldVectorIds);
        }

        List<Document> documents = new ArrayList<>(chunks.size());
        for (KnowledgeChunk chunk : chunks) {
            Map<String, Object> metadata = parseMetadata(chunk.getMetadata());
            metadata.put("source", file.getFileName());
            metadata.put("owner_user_id", file.getOwnerUserId());
            if (file.getTeamId() != null) {
                metadata.put("team_id", file.getTeamId());
            }
            metadata.put("chunk_index", chunk.getChunkIndex());
            metadata.put("chunk_count", chunks.size());
            metadata.put("chunk_size", StringUtils.hasText(chunk.getContent()) ? chunk.getContent().length() : 0);
            documents.add(new Document(chunk.getContent(), metadata));
        }
        vectorStore.add(documents);

        Date now = new Date();
        List<String> newVectorIds = documents.stream().map(Document::getId).collect(Collectors.toList());
        for (int i = 0; i < chunks.size(); i++) {
            KnowledgeChunk chunk = chunks.get(i);
            Document document = documents.get(i);
            chunk.setDocumentId(document.getId());
            chunk.setChunkCount(chunks.size());
            chunk.setChunkSize(StringUtils.hasText(chunk.getContent()) ? chunk.getContent().length() : 0);
            chunk.setMetadata(JSON.toJSONString(document.getMetadata()));
            chunk.setUpdateTime(now);
            knowledgeChunkMapper.updateById(chunk);
        }

        file.setVectorId(JSON.toJSONString(newVectorIds));
        file.setUpdateTime(now);
        aliOssFileMapper.updateById(file);
        return ResultUtils.success("重建索引成功，共写入 " + newVectorIds.size() + " 个向量分块");
    }

    public static String extractFileName(String url) {
        // 找到最后一个斜杠的位置
        int lastSlashIndex = url.lastIndexOf('/');
        if (lastSlashIndex == -1) {
            return url; // 如果没有找到斜杠，返回整个URL
        }
        // 从最后一个斜杠之后的部分截取
        return url.substring(lastSlashIndex + 1);
    }

    private KnowledgeChunkVO toChunkVO(KnowledgeChunk chunk) {
        return KnowledgeChunkVO.builder()
                .id(chunk.getId())
                .fileId(chunk.getFileId())
                .documentId(chunk.getDocumentId())
                .source(chunk.getSource())
                .chunkIndex(chunk.getChunkIndex())
                .chunkCount(chunk.getChunkCount())
                .chunkSize(chunk.getChunkSize())
                .preview(preview(chunk.getContent()))
                .content(chunk.getContent())
                .metadata(parseMetadata(chunk.getMetadata()))
                .build();
    }

    private List<String> parseVectorIds(String vectorId) {
        if (!StringUtils.hasText(vectorId)) {
            return Collections.emptyList();
        }
        try {
            return JSON.parseArray(vectorId, String.class);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private Map<String, Object> parseMetadata(String metadata) {
        if (!StringUtils.hasText(metadata)) {
            return new java.util.LinkedHashMap<>();
        }
        try {
            return JSON.parseObject(metadata, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return new java.util.LinkedHashMap<>();
        }
    }

    private String preview(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        String compactContent = content.replaceAll("\\s+", " ").trim();
        if (compactContent.length() <= PREVIEW_LENGTH) {
            return compactContent;
        }
        return compactContent.substring(0, PREVIEW_LENGTH) + "...";
    }

    private String metadataValue(Map<String, Object> metadata, String key) {
        if (metadata == null || metadata.get(key) == null) {
            return null;
        }
        return metadata.get(key).toString();
    }

    private Integer metadataInteger(Map<String, Object> metadata, String key) {
        if (metadata == null || metadata.get(key) == null) {
            return null;
        }
        Object value = metadata.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private AliOssFile getOwnedFile(Long id) {
        if (id == null) {
            return null;
        }
        return aliOssFileMapper.selectOne(new LambdaQueryWrapper<AliOssFile>()
                .eq(AliOssFile::getId, id)
                .eq(AliOssFile::getOwnerUserId, currentUserId())
                .last("limit 1"));
    }

    private Long currentUserId() {
        return BaseContext.getCurrentId();
    }

}

