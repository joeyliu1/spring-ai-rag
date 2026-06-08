package com.lss.springairag.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lss.springairag.entity.SensitiveWord;
import com.lss.springairag.pojo.vo.SensitiveWordVO;

public interface SensitiveWordService extends IService<SensitiveWord> {

    IPage<SensitiveWordVO> pageWithCategory(Page<SensitiveWordVO> page);

}
