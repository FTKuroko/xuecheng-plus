package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.service.CourseBaseInfoService;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author Kuroko
 * @description 课程信息管理业务接口实现类
 * @date 2023/5/6 16:57
 */
@Service
public class CourseBaseInfoServiceImpl implements CourseBaseInfoService {
    @Resource
    CourseBaseMapper courseBaseMapper;

    /**
     * 课程基本信息查询
     * @param pageParams
     * @param queryCourseParamsDto
     * @return
     */
    @Override
    public PageResult<CourseBase> queryCourseBaseList(PageParams pageParams, QueryCourseParamsDto queryCourseParamsDto) {
        // 构造条件查询器
        LambdaQueryWrapper<CourseBase> lqw = new LambdaQueryWrapper<>();
        // 构建查询条件:按照课程名称模糊查询
        lqw.like(StringUtils.isNotEmpty(queryCourseParamsDto.getCourseName()), CourseBase::getName, queryCourseParamsDto.getCourseName());
        // 构建查询条件:按照课程审核状态查询
        lqw.eq(StringUtils.isNotEmpty(queryCourseParamsDto.getAuditStatus()), CourseBase::getAuditStatus, queryCourseParamsDto.getAuditStatus());
        // 构建查询条件:按照课程发布状态查询
        lqw.eq(StringUtils.isNotEmpty(queryCourseParamsDto.getPublishStatus()), CourseBase::getStatus, queryCourseParamsDto.getPublishStatus());
        // 分页对象
        Page<CourseBase> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());
        // 查询数据内容获得结果
        Page<CourseBase> pageInfo = courseBaseMapper.selectPage(page, lqw);
        // 获取数据列表
        List<CourseBase> records = pageInfo.getRecords();
        // 获取数据总条数
        long total = pageInfo.getTotal();
        // 返回结果集
        return new PageResult<>(records, total, pageParams.getPageNo(), pageParams.getPageSize());
    }
}
