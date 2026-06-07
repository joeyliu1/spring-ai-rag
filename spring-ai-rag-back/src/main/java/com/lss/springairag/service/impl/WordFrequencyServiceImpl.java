package com.lss.springairag.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lss.springairag.common.PageResult;
import com.lss.springairag.entity.WordFrequency;
import com.lss.springairag.mapper.WordFrequencyMapper;
import com.lss.springairag.pojo.dto.WordFrequencyPageQueryDTO;
import com.lss.springairag.service.WordFrequencyService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.wltea.analyzer.core.IKSegmenter;
import org.wltea.analyzer.core.Lexeme;

import java.io.IOException;
import java.io.StringReader;
import java.sql.Date;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class WordFrequencyServiceImpl extends ServiceImpl<WordFrequencyMapper, WordFrequency>
    implements WordFrequencyService {

    private static final Set<String> STOP_WORDS = Set.of(
            "一个", "一些", "一种", "这个", "那个", "什么", "怎么", "如何", "为什么",
            "可以", "是否", "以及", "或者", "如果", "因为", "所以", "关于", "请问",
            "你好", "帮我", "一下", "需要", "实现", "查询", "回答"
    );

    @Override
    public PageResult pageQuery(WordFrequencyPageQueryDTO queryDTO) {
        Page<WordFrequency> page = new Page<>(queryDTO.getPage(), queryDTO.getPageSize());

        LambdaQueryWrapper<WordFrequency> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.isNotEmpty(queryDTO.getWord()),
                        WordFrequency::getWord, queryDTO.getWord())
                .eq(StringUtils.isNotEmpty(queryDTO.getBusinessType()),
                        WordFrequency::getBusinessType, queryDTO.getBusinessType())
                .gt(queryDTO.getCountNumMin() != null,
                        WordFrequency::getCountNum, queryDTO.getCountNumMin())
                .ge(queryDTO.getStartDate() != null,
                        WordFrequency::getCreateTime, toSqlDate(queryDTO.getStartDate()))
                .le(queryDTO.getEndDate() != null,
                        WordFrequency::getCreateTime, toSqlDate(queryDTO.getEndDate()))
                .orderByDesc(WordFrequency::getCountNum);

        this.page(page, wrapper);
        return new PageResult(page.getTotal(), page.getRecords());
    }

    @Override
    public void recordQuestion(String question, String businessType) {
        Map<String, Integer> wordCounts = tokenize(question);
        if (wordCounts.isEmpty()) {
            return;
        }
        Date today = Date.valueOf(LocalDate.now());
        Date now = new Date(System.currentTimeMillis());
        for (Map.Entry<String, Integer> entry : wordCounts.entrySet()) {
            WordFrequency existed = getOne(new LambdaQueryWrapper<WordFrequency>()
                    .eq(WordFrequency::getWord, entry.getKey())
                    .eq(WordFrequency::getBusinessType, businessType)
                    .eq(WordFrequency::getCreateTime, today)
                    .last("limit 1"));
            if (existed == null) {
                WordFrequency wordFrequency = new WordFrequency();
                wordFrequency.setWord(entry.getKey());
                wordFrequency.setCountNum(entry.getValue());
                wordFrequency.setBusinessType(businessType);
                wordFrequency.setCreateTime(today);
                wordFrequency.setUpdateTime(now);
                save(wordFrequency);
            } else {
                existed.setCountNum(existed.getCountNum() + entry.getValue());
                existed.setUpdateTime(now);
                updateById(existed);
            }
        }
    }

    @Override
    public List<WordFrequency> listHotWords(LocalDate startDate, LocalDate endDate, String businessType) {
        LambdaQueryWrapper<WordFrequency> wrapper = new LambdaQueryWrapper<WordFrequency>()
                .eq(StringUtils.isNotEmpty(businessType), WordFrequency::getBusinessType, businessType)
                .ge(startDate != null, WordFrequency::getCreateTime, toSqlDate(startDate))
                .le(endDate != null, WordFrequency::getCreateTime, toSqlDate(endDate))
                .orderByDesc(WordFrequency::getCountNum);
        return list(wrapper);
    }

    private Map<String, Integer> tokenize(String question) {
        Map<String, Integer> wordCounts = new HashMap<>();
        if (StringUtils.isBlank(question)) {
            return wordCounts;
        }
        IKSegmenter segmenter = new IKSegmenter(new StringReader(question), true);
        try {
            Lexeme lexeme;
            while ((lexeme = segmenter.next()) != null) {
                String word = lexeme.getLexemeText();
                if (isValidHotWord(word)) {
                    wordCounts.merge(word, 1, Integer::sum);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("分词失败", e);
        }
        return wordCounts;
    }

    private boolean isValidHotWord(String word) {
        if (StringUtils.isBlank(word)) {
            return false;
        }
        String trimmedWord = word.trim();
        if (trimmedWord.length() < 2 || trimmedWord.length() > 30) {
            return false;
        }
        if (STOP_WORDS.contains(trimmedWord)) {
            return false;
        }
        return !trimmedWord.matches("[\\p{Punct}\\s\\d]+");
    }

    private Date toSqlDate(LocalDate date) {
        return date == null ? null : Date.valueOf(date);
    }
}



