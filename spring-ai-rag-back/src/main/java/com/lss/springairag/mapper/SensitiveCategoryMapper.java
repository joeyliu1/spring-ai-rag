package com.lss.springairag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lss.springairag.entity.SensitiveCategory;
import com.lss.springairag.pojo.vo.SensitiveCategoryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;


@Mapper
public interface SensitiveCategoryMapper extends BaseMapper<SensitiveCategory> {

    List<SensitiveCategoryVO> selectPageWithWordCount(@Param("offset") long offset, @Param("size") long size);

    List<SensitiveCategoryVO> selectListWithWordCount();

    Long countWithWordCount();

}


