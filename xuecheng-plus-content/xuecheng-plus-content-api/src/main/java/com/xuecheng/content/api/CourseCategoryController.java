package com.xuecheng.content.api;

import com.xuecheng.content.model.dto.CourseCategoryTreeDto;
import com.xuecheng.content.service.CourseCatogoryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author Kuroko
 * @description 课程分类接口
 * @date 2023/5/8 11:16
 */
@Slf4j
@RestController
@Api(value = "课程分类相关接口", tags = "课程分类相关接口")
public class CourseCategoryController {
    @Resource
    CourseCatogoryService courseCatogoryService;
    /**
     * 返回课程分类树形结构
     * @return
     */
    @GetMapping("/course-category/tree-nodes")
    @ApiOperation("课程分类相关接口")
    public List<CourseCategoryTreeDto> queryTreeNodes(){
        return courseCatogoryService.queryTreeNodes("1");
    }
}
