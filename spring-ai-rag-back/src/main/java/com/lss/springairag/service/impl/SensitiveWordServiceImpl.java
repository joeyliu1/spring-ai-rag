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
        return baseMapper.selectPageWithCategory(page);
    }

}



