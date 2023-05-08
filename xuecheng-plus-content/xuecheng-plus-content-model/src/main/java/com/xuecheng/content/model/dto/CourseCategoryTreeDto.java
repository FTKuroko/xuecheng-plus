package com.xuecheng.content.model.dto;

import com.xuecheng.content.model.po.CourseCategory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author Kuroko
 * @description 课程分类树形节点 Dto
 * @date 2023/5/8 11:15
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CourseCategoryTreeDto extends CourseCategory {
    List childrenTreeNodes;
}
