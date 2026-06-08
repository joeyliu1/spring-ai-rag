package com.lss.springairag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lss.springairag.entity.SensitiveWord;
import com.lss.springairag.pojo.vo.SensitiveWordVO;
import org.apache.ibatis.annotations.Mapper;


@Mapper
public interface SensitiveWordMapper extends BaseMapper<SensitiveWord> {

    IPage<SensitiveWordVO> selectPageWithCategory(Page<SensitiveWordVO> page);

}



