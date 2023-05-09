package com.xuecheng.content.service;

import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;

import java.util.List;

/**
 * @author Kuroko
 * @description
 * @date 2023/5/9 16:01
 */
public interface TeachplanService {
    /**
     * 根据课程 id 查询课程计划树形结构
     * @param courseId
     * @return
     */
    List<TeachplanDto> findTeachplanTree(Long courseId);

    /**
     * 新增或修改课程计划
     * @param teachplanDto
     */
    void saveTeachplan(SaveTeachplanDto teachplanDto);

    /**
     * 根据课程计划 id 删除课程计划
     * @param teachplanId
     */
    void deleteTeachplan(Long teachplanId);

    /**
     * 课程计划排序，上移下移功能
     * @param moveType
     * @param teachplanId
     */
    void orderByTeachplan(String moveType, Long teachplanId);
}
