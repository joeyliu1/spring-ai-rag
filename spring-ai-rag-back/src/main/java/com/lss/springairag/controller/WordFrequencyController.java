package com.lss.springairag.controller;

import com.lss.springairag.common.ApplicationConstant;
import com.lss.springairag.common.BaseResponse;
import com.lss.springairag.common.PageResult;
import com.lss.springairag.common.ResultUtils;
import com.lss.springairag.entity.WordFrequency;
import com.lss.springairag.pojo.dto.WordFrequencyPageQueryDTO;
import com.lss.springairag.service.WordFrequencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;


@Tag(name = "WordFrequencyController", description = "分词统计控制器")
@Slf4j
@RestController
@RequestMapping(ApplicationConstant.API_VERSION + "/frequency")
public class WordFrequencyController {
    @Autowired
    private WordFrequencyService wordFrequencyService;

    // 分页条件查询
    @PostMapping("/page")
    @Operation(summary = "page", description = "分页查询")
    public BaseResponse<PageResult> pageQuery(@RequestBody WordFrequencyPageQueryDTO queryDTO) {
        PageResult pageResult = wordFrequencyService.pageQuery(queryDTO);
        pageResult.setTotal(pageResult.getRecords().size());
        return ResultUtils.success(pageResult);
    }

    // 清空数据
    @DeleteMapping("/clean")
    @Operation(summary = "clean", description = "清空数据")
    public BaseResponse<String> clean() {
        wordFrequencyService.remove(null);
        return ResultUtils.success("清空成功");
    }


    @GetMapping("/getList")
    @Operation(summary = "getList", description = "查询真实问答热词，支持日期范围")
    public BaseResponse<List<WordFrequency>> getList(@RequestParam(required = false) LocalDate startDate,
                                                     @RequestParam(required = false) LocalDate endDate,
                                                     @RequestParam(required = false) String businessType) {
        return ResultUtils.success(wordFrequencyService.listHotWords(startDate, endDate, businessType));
    }
}
