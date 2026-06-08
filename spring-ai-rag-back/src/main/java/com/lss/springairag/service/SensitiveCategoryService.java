package com.lss.springairag.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lss.springairag.entity.SensitiveCategory;
import com.lss.springairag.pojo.vo.SensitiveCategoryVO;

public interface SensitiveCategoryService extends IService<SensitiveCategory> {

    IPage<SensitiveCategoryVO> pageWithWordCount(Page<SensitiveCategoryVO> page);

    java.util.List<SensitiveCategoryVO> listWithWordCount();

}
