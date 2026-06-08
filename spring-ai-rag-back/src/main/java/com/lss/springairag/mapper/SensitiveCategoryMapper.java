package com.lss.springairag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lss.springairag.entity.SensitiveCategory;
import com.lss.springairag.pojo.vo.SensitiveCategoryVO;
import org.apache.ibatis.annotations.Mapper;


@Mapper
public interface SensitiveCategoryMapper extends BaseMapper<SensitiveCategory> {

    IPage<SensitiveCategoryVO> selectPageWithWordCount(Page<SensitiveCategoryVO> page);

    java.util.List<SensitiveCategoryVO> selectListWithWordCount();

}



