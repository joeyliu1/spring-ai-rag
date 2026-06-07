package com.lss.springairag.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lss.springairag.common.ApplicationConstant;
import com.lss.springairag.common.BaseResponse;
import com.lss.springairag.common.ResultUtils;
import com.lss.springairag.entity.SensitiveAuditLog;
import com.lss.springairag.pojo.vo.SensitiveCategoryStatVO;
import com.lss.springairag.service.SensitiveAuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "SensitiveAuditController", description = "敏感词审核日志与统计")
@RestController
@RequestMapping(ApplicationConstant.API_VERSION + "/sensitive/audit")
public class SensitiveAuditController {

    private final SensitiveAuditService sensitiveAuditService;

    public SensitiveAuditController(SensitiveAuditService sensitiveAuditService) {
        this.sensitiveAuditService = sensitiveAuditService;
    }

    @Operation(summary = "分页查询敏感词命中日志")
    @GetMapping("/page")
    public BaseResponse<IPage<SensitiveAuditLog>> page(@RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "10") int size,
                                                       @RequestParam(required = false) String direction,
                                                       @RequestParam(required = false) String riskLevel,
                                                       @RequestParam(required = false) String action) {
        return ResultUtils.success(sensitiveAuditService.pageAuditLogs(page, size, direction, riskLevel, action));
    }

    @Operation(summary = "按分类统计敏感词命中情况")
    @GetMapping("/category-stats")
    public BaseResponse<List<SensitiveCategoryStatVO>> categoryStats() {
        return ResultUtils.success(sensitiveAuditService.categoryStats());
    }
}
