package com.xuecheng.content.service;

import com.xuecheng.content.model.dto.CourseCategoryTreeDto;

import java.util.List;

/**
 * @author Kuroko
 * @description
 * @date 2023/5/8 14:49
 */
public interface CourseCatogoryService {
    /**
     * 课程分类树形结构查询
     * @param id    根节点 id
     * @return      返回根节点下面所有子节点
     */
    public List<CourseCategoryTreeDto> queryTreeNodes(String id);
}
