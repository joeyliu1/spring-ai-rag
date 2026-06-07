package com.lss.springairag.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lss.springairag.context.BaseContext;
import com.lss.springairag.entity.SensitiveAuditLog;
import com.lss.springairag.entity.SensitiveWord;
import com.lss.springairag.mapper.SensitiveAuditLogMapper;
import com.lss.springairag.pojo.vo.SensitiveAuditResult;
import com.lss.springairag.pojo.vo.SensitiveCategoryStatVO;
import com.lss.springairag.service.SensitiveAuditService;
import com.lss.springairag.service.SensitiveWordService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class SensitiveAuditServiceImpl extends ServiceImpl<SensitiveAuditLogMapper, SensitiveAuditLog>
        implements SensitiveAuditService {

    private static final String DIRECTION_INPUT = "INPUT";
    private static final String DIRECTION_OUTPUT = "OUTPUT";
    private static final String ACTION_BLOCK = "BLOCK";

    private final SensitiveWordService sensitiveWordService;

    public SensitiveAuditServiceImpl(SensitiveWordService sensitiveWordService) {
        this.sensitiveWordService = sensitiveWordService;
    }

    @Override
    public SensitiveAuditResult auditInput(String content, String scene) {
        return audit(content, DIRECTION_INPUT, scene);
    }

    @Override
    public SensitiveAuditResult auditOutput(String content, String scene) {
        return audit(content, DIRECTION_OUTPUT, scene);
    }

    @Override
    public IPage<SensitiveAuditLog> pageAuditLogs(int page, int size, String direction, String riskLevel, String action) {
        Page<SensitiveAuditLog> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<SensitiveAuditLog> wrapper = new LambdaQueryWrapper<SensitiveAuditLog>()
                .eq(StringUtils.hasText(direction), SensitiveAuditLog::getDirection, direction)
                .eq(StringUtils.hasText(riskLevel), SensitiveAuditLog::getRiskLevel, riskLevel)
                .eq(StringUtils.hasText(action), SensitiveAuditLog::getAction, action)
                .orderByDesc(SensitiveAuditLog::getCreateTime);
        return page(pageParam, wrapper);
    }

    @Override
    public List<SensitiveCategoryStatVO> categoryStats() {
        List<SensitiveAuditLog> logs = list();
        Map<String, CategoryStat> statMap = new LinkedHashMap<>();
        for (SensitiveAuditLog log : logs) {
            String category = defaultText(log.getCategory(), "未分类");
            String riskLevel = defaultText(log.getRiskLevel(), "LOW");
            String key = category + "#" + riskLevel;
            CategoryStat stat = statMap.computeIfAbsent(key, unused -> new CategoryStat(category, riskLevel));
            stat.hitCount++;
            if (ACTION_BLOCK.equals(log.getAction())) {
                stat.blockCount++;
            }
        }
        return statMap.values().stream()
                .map(stat -> SensitiveCategoryStatVO.builder()
                        .category(stat.category)
                        .riskLevel(stat.riskLevel)
                        .hitCount(stat.hitCount)
                        .blockCount(stat.blockCount)
                        .build())
                .toList();
    }

    private SensitiveAuditResult audit(String content, String direction, String scene) {
        List<SensitiveAuditResult.HitWord> hits = findHits(content);
        if (hits.isEmpty()) {
            return SensitiveAuditResult.builder()
                    .hit(false)
                    .blocked(false)
                    .riskLevel("NONE")
                    .hitWords(List.of())
                    .build();
        }

        String riskLevel = highestRiskLevel(hits);
        boolean blocked = true;
        for (SensitiveAuditResult.HitWord hit : hits) {
            saveAuditLog(content, direction, scene, hit, blocked);
        }

        return SensitiveAuditResult.builder()
                .hit(true)
                .blocked(blocked)
                .riskLevel(riskLevel)
                .blockMessage(buildBlockMessage(direction, riskLevel, hits))
                .hitWords(hits)
                .build();
    }

    private List<SensitiveAuditResult.HitWord> findHits(String content) {
        if (!StringUtils.hasText(content)) {
            return List.of();
        }
        String normalizedContent = content.toLowerCase(Locale.ROOT);
        List<SensitiveAuditResult.HitWord> hits = new ArrayList<>();
        for (SensitiveWord sensitiveWord : sensitiveWordService.list()) {
            String word = sensitiveWord.getWord();
            if (!StringUtils.hasText(word)) {
                continue;
            }
            if (!"1".equals(sensitiveWord.getStatus())) {
                continue;
            }
            if (normalizedContent.contains(word.toLowerCase(Locale.ROOT))) {
                String category = defaultText(sensitiveWord.getCategory(), "未分类");
                hits.add(SensitiveAuditResult.HitWord.builder()
                        .word(word)
                        .category(category)
                        .riskLevel(resolveRiskLevel(category))
                        .build());
            }
        }
        return hits;
    }

    private void saveAuditLog(String content,
                              String direction,
                              String scene,
                              SensitiveAuditResult.HitWord hit,
                              boolean blocked) {
        SensitiveAuditLog log = new SensitiveAuditLog();
        log.setUserId(BaseContext.getCurrentId());
        log.setDirection(direction);
        log.setScene(scene);
        log.setWord(hit.getWord());
        log.setCategory(hit.getCategory());
        log.setRiskLevel(hit.getRiskLevel());
        log.setAction(blocked ? ACTION_BLOCK : "PASS");
        log.setContentPreview(preview(content));
        log.setCreateTime(LocalDateTime.now());
        save(log);
    }

    private String buildBlockMessage(String direction, String riskLevel, List<SensitiveAuditResult.HitWord> hits) {
        String target = DIRECTION_INPUT.equals(direction) ? "你的问题" : "AI 回复";
        return target + "命中敏感词审核规则，已拦截。风险等级：" + riskLevel + "，命中词：" + hits.get(0).getWord();
    }

    private String highestRiskLevel(List<SensitiveAuditResult.HitWord> hits) {
        if (hits.stream().anyMatch(hit -> "HIGH".equals(hit.getRiskLevel()))) {
            return "HIGH";
        }
        if (hits.stream().anyMatch(hit -> "MEDIUM".equals(hit.getRiskLevel()))) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private String resolveRiskLevel(String category) {
        String normalizedCategory = defaultText(category, "").toLowerCase(Locale.ROOT);
        if (normalizedCategory.contains("高危")
                || normalizedCategory.contains("违法")
                || normalizedCategory.contains("违禁")
                || normalizedCategory.contains("涉政")
                || normalizedCategory.contains("政治")
                || normalizedCategory.contains("暴力")
                || normalizedCategory.contains("色情")
                || normalizedCategory.contains("攻击")
                || normalizedCategory.contains("注入")
                || normalizedCategory.contains("越狱")
                || normalizedCategory.contains("隐私")
                || normalizedCategory.contains("泄露")) {
            return "HIGH";
        }
        if (normalizedCategory.contains("中危")
                || normalizedCategory.contains("风险")
                || normalizedCategory.contains("敏感")) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private String preview(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        String compactContent = content.replaceAll("\\s+", " ").trim();
        if (compactContent.length() <= 300) {
            return compactContent;
        }
        return compactContent.substring(0, 300) + "...";
    }

    private String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private static class CategoryStat {
        private final String category;
        private final String riskLevel;
        private long hitCount;
        private long blockCount;

        private CategoryStat(String category, String riskLevel) {
            this.category = category;
            this.riskLevel = riskLevel;
        }
    }
}
