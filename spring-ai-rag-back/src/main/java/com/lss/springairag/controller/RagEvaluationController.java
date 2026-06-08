package com.lss.springairag.controller;

import com.lss.springairag.common.ApplicationConstant;
import com.lss.springairag.common.BaseResponse;
import com.lss.springairag.common.ErrorCode;
import com.lss.springairag.common.ResultUtils;
import com.lss.springairag.pojo.dto.RagEvaluationBatchRequest;
import com.lss.springairag.pojo.dto.RagEvaluationRequest;
import com.lss.springairag.service.RagEvaluationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "RagEvaluationController", description = "RAG 检索质量评估接口")
@RestController
@RequestMapping(ApplicationConstant.API_VERSION + "/rag")
public class RagEvaluationController {

    private final RagEvaluationService ragEvaluationService;

    public RagEvaluationController(RagEvaluationService ragEvaluationService) {
        this.ragEvaluationService = ragEvaluationService;
    }

    @Operation(summary = "evaluate", description = "评估 RAG 向量检索命中情况")
    @PostMapping("/evaluate")
    public BaseResponse evaluate(@RequestBody RagEvaluationRequest request) {
        if (request == null || !StringUtils.hasText(request.getQuestion())) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR, "question 不能为空");
        }
        return ResultUtils.success(ragEvaluationService.evaluate(request));
    }

    @Operation(summary = "batchEvaluate", description = "批量评估 RAG 检索质量并对比 topK 和相似度阈值策略")
    @PostMapping("/evaluate/batch")
    public BaseResponse batchEvaluate(@RequestBody RagEvaluationBatchRequest request) {
        if (request == null || CollectionUtils.isEmpty(request.getCases())) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR, "cases 不能为空");
        }
        boolean hasQuestion = request.getCases().stream()
                .anyMatch(testCase -> testCase != null && StringUtils.hasText(testCase.getQuestion()));
        if (!hasQuestion) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR, "至少需要一个有效 question");
        }
        return ResultUtils.success(ragEvaluationService.evaluateBatch(request));
    }
}
