package com.lss.springairag.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lss.springairag.entity.SensitiveAuditLog;
import com.lss.springairag.pojo.vo.SensitiveAuditResult;
import com.lss.springairag.pojo.vo.SensitiveCategoryStatVO;

import java.util.List;

public interface SensitiveAuditService extends IService<SensitiveAuditLog> {

    SensitiveAuditResult auditInput(String content, String scene);

    SensitiveAuditResult auditOutput(String content, String scene);

    IPage<SensitiveAuditLog> pageAuditLogs(int page, int size, String direction, String riskLevel, String action);

    List<SensitiveCategoryStatVO> categoryStats();

    void refreshMatcher();
}
