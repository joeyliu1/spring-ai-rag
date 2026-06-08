package com.lss.springairag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lss.springairag.entity.SensitiveWord;
import com.lss.springairag.pojo.vo.SensitiveWordVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;


@Mapper
public interface SensitiveWordMapper extends BaseMapper<SensitiveWord> {

    List<SensitiveWordVO> selectPageWithCategory(@Param("offset") long offset, @Param("size") long size);

    Long countWithCategory();

}


