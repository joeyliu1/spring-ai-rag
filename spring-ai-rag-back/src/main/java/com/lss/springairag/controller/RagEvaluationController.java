package com.lss.springairag.controller;

import com.lss.springairag.common.ApplicationConstant;
import com.lss.springairag.common.BaseResponse;
import com.lss.springairag.common.ErrorCode;
import com.lss.springairag.common.ResultUtils;
import com.lss.springairag.pojo.dto.RagEvaluationRequest;
import com.lss.springairag.service.RagEvaluationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
}
