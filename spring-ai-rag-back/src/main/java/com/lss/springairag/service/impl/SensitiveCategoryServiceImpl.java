package com.lss.springairag.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lss.springairag.entity.SensitiveCategory;
import com.lss.springairag.mapper.SensitiveCategoryMapper;
import com.lss.springairag.pojo.vo.SensitiveCategoryVO;
import com.lss.springairag.service.SensitiveCategoryService;
import org.springframework.stereotype.Service;

@Service
public class SensitiveCategoryServiceImpl extends ServiceImpl<SensitiveCategoryMapper, SensitiveCategory>
    implements SensitiveCategoryService {

    @Override
    public IPage<SensitiveCategoryVO> pageWithWordCount(Page<SensitiveCategoryVO> page) {
        return baseMapper.selectPageWithWordCount(page);
    }

    @Override
    public java.util.List<SensitiveCategoryVO> listWithWordCount() {
        return baseMapper.selectListWithWordCount();
    }

}



