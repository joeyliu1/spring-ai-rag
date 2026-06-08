package com.lss.springairag.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lss.springairag.common.ApplicationConstant;
import com.lss.springairag.common.BaseResponse;
import com.lss.springairag.common.ResultUtils;
import com.lss.springairag.entity.SensitiveWord;
import com.lss.springairag.pojo.vo.SensitiveWordVO;
import com.lss.springairag.service.SensitiveCategoryService;
import com.lss.springairag.service.SensitiveWordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;


@Tag(name = "SensitiveWordController", description = "敏感词控制器")
@Slf4j
@RestController
@RequestMapping(ApplicationConstant.API_VERSION + "/sensitive")
public class SensitiveWordController {

    @Autowired
    private SensitiveWordService sensitiveWordService;

    @Autowired
    private SensitiveCategoryService sensitiveCategoryService;

    @Operation(summary = "新增敏感词")
    @PostMapping("/add")
    public BaseResponse addSensitiveWord(@RequestBody SensitiveWord sensitiveWord) {
        log.info("新增敏感词：{}", sensitiveWord);
        if (sensitiveWord.getCategoryId() == null) {
            return ResultUtils.error("请选择敏感词分类");
        }
        if (sensitiveCategoryService.getById(sensitiveWord.getCategoryId()) == null) {
            return ResultUtils.error("敏感词分类不存在");
        }
        sensitiveWord.setStatus("1");
        sensitiveWord.setCreatedAt(LocalDate.now().toString());
        sensitiveWord.setUpdatedAt(LocalDate.now().toString());
        boolean save = sensitiveWordService.save(sensitiveWord);
        if (save){
            return ResultUtils.success(true);
        }
        return ResultUtils.error("新增失败");
    }

    @Operation(summary = "删除敏感词")
    @DeleteMapping("/{id}")
    public boolean deleteSensitiveWord(@PathVariable Long id) {
        return sensitiveWordService.removeById(id);
    }

    @Operation(summary = "批量删除敏感词")
    @PostMapping("/batch")
    public BaseResponse deleteSensitiveWords(@RequestBody List<Long> ids) {
        boolean b = sensitiveWordService.removeByIds(ids);
        if (b){
            return ResultUtils.success("删除成功");
        }
        return ResultUtils.error("删除失败");
    }

    @Operation(summary = "更新敏感词")
    @PutMapping
    public boolean updateSensitiveWord(@RequestBody SensitiveWord sensitiveWord) {
        return sensitiveWordService.updateById(sensitiveWord);
    }

    @Operation(summary = "分页查询敏感词")
    @GetMapping("/page")
    public BaseResponse<IPage<SensitiveWordVO>> getSensitiveWordPage(@RequestParam int page, @RequestParam int size) {
        Page<SensitiveWordVO> pageParam = new Page<>(page, size);
        return ResultUtils.success(sensitiveWordService.pageWithCategory(pageParam));
    }

    @Operation(summary = "查询所有敏感词")
    @GetMapping
    public List<SensitiveWordVO> getAllSensitiveWords() {
        return sensitiveWordService.pageWithCategory(new Page<>(1, Long.MAX_VALUE)).getRecords();
    }


}
