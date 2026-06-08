package com.lss.springairag.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lss.springairag.common.ApplicationConstant;
import com.lss.springairag.common.BaseResponse;
import com.lss.springairag.common.ResultUtils;
import com.lss.springairag.entity.SensitiveWord;
import com.lss.springairag.entity.SensitiveCategory;
import com.lss.springairag.pojo.vo.SensitiveCategoryVO;
import com.lss.springairag.service.SensitiveCategoryService;
import com.lss.springairag.service.SensitiveWordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;


@Tag(name = "SensitiveCategoryController", description = "敏感词分类控制器")
@Slf4j
@RestController
@RequestMapping(ApplicationConstant.API_VERSION + "/category")
public class SensitiveCategoryController {
    @Autowired
    private SensitiveCategoryService sensitiveCategoryService;

    @Autowired
    private SensitiveWordService sensitiveWordService;

    // 新增接口
    @Operation(summary = "新增敏感词分类")
    @PostMapping("/add")
    public BaseResponse<Boolean> create(@RequestBody SensitiveCategory entity) {
        entity.setCreatedTime(LocalDate.now());
        entity.setUpdateTime(LocalDate.now());
        entity.setStatus("1");
        return ResultUtils.success(sensitiveCategoryService.save(entity));
    }

    // 批量删除接口
    @Operation(summary = "批量删除")
    @DeleteMapping("/batch")
    public BaseResponse<Boolean> batchDelete(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return ResultUtils.error("请选择要删除的分类");
        }
        long relatedCount = sensitiveWordService.count(new LambdaQueryWrapper<SensitiveWord>()
                .in(SensitiveWord::getCategoryId, ids));
        if (relatedCount > 0) {
            return ResultUtils.error("当前分类已被敏感词引用，不能删除");
        }
        return ResultUtils.success(sensitiveCategoryService.removeByIds(ids));
    }

    // 修改接口
    @Operation(summary = "修改敏感词")
    @PutMapping("/update")
    public BaseResponse<Boolean> update(@RequestBody SensitiveCategory entity) {
        entity.setUpdateTime(LocalDate.now());
        return ResultUtils.success(sensitiveCategoryService.updateById(entity));
    }

    // 分页查询接口
    @Operation(summary = "分页查询")
    @GetMapping("/page")
    public BaseResponse<IPage<SensitiveCategoryVO>> page(@RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "10") int size) {
        Page<SensitiveCategoryVO> pageParam = new Page<>(page, size);
        return ResultUtils.success(sensitiveCategoryService.pageWithWordCount(pageParam));
    }

    // 列表查询接口
    @Operation(summary = "获取全部列表")
    @GetMapping("/list")
    public BaseResponse<List<SensitiveCategoryVO>> list() {
        return ResultUtils.success(sensitiveCategoryService.listWithWordCount());
    }


}
