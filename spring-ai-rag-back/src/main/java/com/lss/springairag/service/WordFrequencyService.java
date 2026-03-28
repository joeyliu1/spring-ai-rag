package com.lss.springairag.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lss.springairag.common.PageResult;
import com.lss.springairag.entity.WordFrequency;
import com.lss.springairag.pojo.dto.WordFrequencyPageQueryDTO;

public interface WordFrequencyService extends IService<WordFrequency> {

    PageResult pageQuery(WordFrequencyPageQueryDTO queryDTO);
}
