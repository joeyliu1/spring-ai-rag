package com.lss.springairag.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lss.springairag.context.BaseContext;
import com.lss.springairag.entity.SensitiveAuditLog;
import com.lss.springairag.mapper.SensitiveAuditLogMapper;
import com.lss.springairag.pojo.vo.SensitiveAuditResult;
import com.lss.springairag.pojo.vo.SensitiveCategoryStatVO;
import com.lss.springairag.pojo.vo.SensitiveWordVO;
import com.lss.springairag.service.SensitiveAuditService;
import com.lss.springairag.service.SensitiveWordService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

@Service
public class SensitiveAuditServiceImpl extends ServiceImpl<SensitiveAuditLogMapper, SensitiveAuditLog>
        implements SensitiveAuditService {

    private static final String DIRECTION_INPUT = "INPUT";
    private static final String DIRECTION_OUTPUT = "OUTPUT";
    private static final String ACTION_BLOCK = "BLOCK";

    private final SensitiveWordService sensitiveWordService;

    private volatile SensitiveWordMatcher sensitiveWordMatcher;

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

    @Override
    public void refreshMatcher() {
        sensitiveWordMatcher = buildMatcher();
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
        SensitiveWordMatcher matcher = sensitiveWordMatcher;
        if (matcher == null) {
            synchronized (this) {
                matcher = sensitiveWordMatcher;
                if (matcher == null) {
                    matcher = buildMatcher();
                    sensitiveWordMatcher = matcher;
                }
            }
        }
        return matcher.find(content);
    }

    private SensitiveWordMatcher buildMatcher() {
        List<SensitiveWordVO> words = sensitiveWordService.pageWithCategory(new Page<>(1, Long.MAX_VALUE)).getRecords();
        List<SensitiveWordRule> rules = words.stream()
                .filter(word -> word != null && StringUtils.hasText(word.getWord()))
                .filter(word -> "1".equals(word.getStatus()))
                .map(word -> {
                    String category = defaultText(word.getCategoryName(), "未分类");
                    return new SensitiveWordRule(
                            word.getWord(),
                            word.getWord().toLowerCase(Locale.ROOT),
                            category,
                            resolveRiskLevel(category)
                    );
                })
                .toList();
        return new SensitiveWordMatcher(rules);
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

    private record SensitiveWordRule(String word, String normalizedWord, String category, String riskLevel) {
    }

    private static class SensitiveWordMatcher {

        private final MatchNode root = new MatchNode();

        private SensitiveWordMatcher(List<SensitiveWordRule> rules) {
            for (SensitiveWordRule rule : rules) {
                addRule(rule);
            }
            buildFailureLinks();
        }

        private List<SensitiveAuditResult.HitWord> find(String content) {
            if (root.children.isEmpty()) {
                return List.of();
            }
            String normalizedContent = content.toLowerCase(Locale.ROOT);
            List<SensitiveAuditResult.HitWord> hits = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            MatchNode current = root;
            for (int i = 0; i < normalizedContent.length(); i++) {
                char ch = normalizedContent.charAt(i);
                while (current != root && !current.children.containsKey(ch)) {
                    current = current.failure;
                }
                current = current.children.getOrDefault(ch, root);
                if (current.outputs.isEmpty()) {
                    continue;
                }
                for (SensitiveWordRule rule : current.outputs) {
                    String key = rule.word() + "#" + rule.category();
                    if (!seen.add(key)) {
                        continue;
                    }
                    hits.add(SensitiveAuditResult.HitWord.builder()
                            .word(rule.word())
                            .category(rule.category())
                            .riskLevel(rule.riskLevel())
                            .build());
                }
            }
            return hits;
        }

        private void addRule(SensitiveWordRule rule) {
            MatchNode current = root;
            for (int i = 0; i < rule.normalizedWord().length(); i++) {
                char ch = rule.normalizedWord().charAt(i);
                current = current.children.computeIfAbsent(ch, unused -> new MatchNode());
            }
            current.outputs.add(rule);
        }

        private void buildFailureLinks() {
            Queue<MatchNode> queue = new ArrayDeque<>();
            root.failure = root;
            for (MatchNode child : root.children.values()) {
                child.failure = root;
                queue.add(child);
            }
            while (!queue.isEmpty()) {
                MatchNode current = queue.poll();
                for (Map.Entry<Character, MatchNode> entry : current.children.entrySet()) {
                    char ch = entry.getKey();
                    MatchNode child = entry.getValue();
                    MatchNode failure = current.failure;
                    while (failure != root && !failure.children.containsKey(ch)) {
                        failure = failure.failure;
                    }
                    child.failure = failure.children.getOrDefault(ch, root);
                    child.outputs.addAll(child.failure.outputs);
                    queue.add(child);
                }
            }
        }
    }

    private static class MatchNode {

        private final Map<Character, MatchNode> children = new LinkedHashMap<>();

        private final List<SensitiveWordRule> outputs = new ArrayList<>();

        private MatchNode failure;
    }
}
