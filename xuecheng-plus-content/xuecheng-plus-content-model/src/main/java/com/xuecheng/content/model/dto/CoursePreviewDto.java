package com.xuecheng.content.model.dto;

import com.xuecheng.content.model.po.CourseBase;
import lombok.Data;

import java.util.List;

/**
 * @author Kuroko
 * @description 课程预览数据模型
 * @date 2023/5/18 16:09
 */
@Data
public class CoursePreviewDto {
    /**
     * CourseBaseInfoDto 包括了课程基本信息和课程营销信息
     */
    CourseBaseInfoDto courseBase;
    /**
     * List<TeachplanDto> 包括了课程计划列表
     */
    List<TeachplanDto> teachplans;
    /**
     * 师资信息，未开发
     */
}
