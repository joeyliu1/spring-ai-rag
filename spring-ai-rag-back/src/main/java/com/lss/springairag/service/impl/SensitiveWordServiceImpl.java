package com.lss.springairag.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lss.springairag.entity.SensitiveWord;
import com.lss.springairag.mapper.SensitiveWordMapper;
import com.lss.springairag.pojo.vo.SensitiveWordVO;
import com.lss.springairag.service.SensitiveWordService;
import org.springframework.stereotype.Service;

@Service
public class SensitiveWordServiceImpl extends ServiceImpl<SensitiveWordMapper, SensitiveWord>
    implements SensitiveWordService {

    @Override
    public IPage<SensitiveWordVO> pageWithCategory(Page<SensitiveWordVO> page) {
        long current = Math.max(page.getCurrent(), 1);
        long size = Math.max(page.getSize(), 1);
        long offset = (current - 1) * size;
        Long total = baseMapper.countWithCategory();

        Page<SensitiveWordVO> result = new Page<>(current, size, total == null ? 0 : total);
        result.setRecords(baseMapper.selectPageWithCategory(offset, size));
        return result;
    }

}


