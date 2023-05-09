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

    void saveTeachplan(SaveTeachplanDto teachplanDto);
}
