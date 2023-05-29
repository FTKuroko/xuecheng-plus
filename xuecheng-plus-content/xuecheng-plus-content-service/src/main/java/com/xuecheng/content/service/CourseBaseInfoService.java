package com.xuecheng.content.service;

import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.model.dto.AddCourseDto;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.EditCourseDto;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;

/**
 * @author Kuroko
 * @description 课程基本信息管理业务接口
 * @date 2023/5/6 16:55
 */
public interface CourseBaseInfoService {
    /**
     * 课程分页查询
     * @param companyId  教学机构 id
     * @param pageParams 分页查询参数
     * @param queryCourseParamsDto 查询条件
     * @return
     */
    public PageResult<CourseBase> queryCourseBaseList(Long companyId, PageParams pageParams, QueryCourseParamsDto queryCourseParamsDto);

    /**
     * 新增课程基本信息
     * @param companyId 教学机构 id
     * @param addCourseDto 课程基本信息
     * @return
     */
    public CourseBaseInfoDto createCourseBase(Long companyId, AddCourseDto addCourseDto);

    /**
     * 根据课程id查询课程基本信息
     * @param courseId
     * @return
     */
    public CourseBaseInfoDto getCourseBaseInfo(Long courseId);

    /**
     * 修改课程信息
     * @param companyId 教学机构id，本机构只能修改本机构的课程
     * @param editCourseDto 修改内容
     * @return
     */
    public CourseBaseInfoDto updateCourseBase(Long companyId, EditCourseDto editCourseDto);

    /**
     * 删除课程
     * @param companyId 教学机构 id，本机构只能删除本机构的课程
     * @param courseId 要删除的课程 id
     */
    void delectCourse(Long companyId, Long courseId);
}
