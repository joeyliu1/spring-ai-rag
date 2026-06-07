package com.lss.springairag.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lss.springairag.common.PageResult;
import com.lss.springairag.entity.WordFrequency;
import com.lss.springairag.pojo.dto.WordFrequencyPageQueryDTO;

import java.time.LocalDate;
import java.util.List;

public interface WordFrequencyService extends IService<WordFrequency> {

    PageResult pageQuery(WordFrequencyPageQueryDTO queryDTO);

    void recordQuestion(String question, String businessType);

    List<WordFrequency> listHotWords(LocalDate startDate, LocalDate endDate, String businessType);
}
