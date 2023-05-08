package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.mapper.CourseMarketMapper;
import com.xuecheng.content.model.dto.AddCourseDto;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.model.po.CourseCategory;
import com.xuecheng.content.model.po.CourseMarket;
import com.xuecheng.content.service.CourseBaseInfoService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
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
    @Resource
    CourseMarketMapper courseMarketMapper;
    @Resource
    CourseCategoryMapper courseCategoryMapper;

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

    /**
     * 添加课程基本信息
     * @param companyId 教学机构 id
     * @param addCourseDto 课程基本信息
     * @return
     */
    @Override
    @Transactional
    public CourseBaseInfoDto createCourseBase(Long companyId, AddCourseDto addCourseDto) {
        // 1.合法性校验
        if(StringUtils.isBlank(addCourseDto.getName())){
            throw new XueChengPlusException("课程名称为空!");
        }
        if(StringUtils.isBlank(addCourseDto.getMt())){
            throw new XueChengPlusException("课程分类为空!");
        }
        if(StringUtils.isBlank(addCourseDto.getSt())){
            throw new XueChengPlusException("课程分类为空!");
        }
        if(StringUtils.isBlank(addCourseDto.getGrade())){
            throw new XueChengPlusException("课程等级为空!");
        }
        if(StringUtils.isBlank(addCourseDto.getUsers())){
            throw new XueChengPlusException("适用人群为空!");
        }
        if(StringUtils.isBlank(addCourseDto.getTeachmode())){
            throw new XueChengPlusException("教育模式为空!");
        }
        if(StringUtils.isBlank(addCourseDto.getCharge())){
            throw new XueChengPlusException("收费规则为空!");
        }
        // 2.封装请求参数
        // 封装课程基本信息
        CourseBase courseBase = new CourseBase();
        // 拷贝相同属性。不同属性手动赋值
        BeanUtils.copyProperties(addCourseDto, courseBase);
        // 2.1设置审核状态。初始化为未审核
        courseBase.setAuditStatus("202002");
        // 2.2设置发布状态，初始状态为未发布
        courseBase.setStatus("203001");
        // 2.3设置机构id
        courseBase.setCompanyId(companyId);
        // 2.4添加时间
        courseBase.setCreateDate(LocalDateTime.now());
        // 2.5插入课程基本信息表
        int insert = courseBaseMapper.insert(courseBase);
        if(insert <= 0){
            throw new RuntimeException("新增课程基本信息失败!");
        }
        // 2.6向课程营销表保存课程营销信息
        // 2.6.1封装课程营销信息
        CourseMarket courseMarket = new CourseMarket();
        Long courseId = courseBase.getId();
        BeanUtils.copyProperties(addCourseDto, courseMarket);
        courseMarket.setId(courseId);
        // 2.6.2判断收费规则，若课程收费，则价格必须大于0
        String charge = courseMarket.getCharge();
        if(StringUtils.isBlank(charge)){
            throw new XueChengPlusException("收费规则没有选择!");
        }
        if("201001".equals(charge)){
            Float price = addCourseDto.getPrice();
            if(price == null || price.floatValue() <= 0){
                throw new XueChengPlusException("课程设置了收费，价格不合法!");
            }
        }
        // 2.6.2根据营销id从课程营销表查询，如果之前没有该课程，则是新增；如果之前已经有该课程，则是更新
        Long courseMarketId = courseMarket.getId();
        CourseMarket courseMarketObj = courseMarketMapper.selectById(courseMarketId);
        int courseMarketInsert = 0;
        if(courseMarketObj == null){
            courseMarketInsert = courseMarketMapper.insert(courseMarket);
        }else{
            BeanUtils.copyProperties(courseMarket, courseMarketObj);
            courseMarketObj.setId(courseMarketId);
            courseMarketInsert = courseMarketMapper.updateById(courseMarketObj);
        }
        if(courseMarketInsert <= 0){
            throw new XueChengPlusException("保存课程营销信息失败!");
        }
        // 3.返回添加的课程信息及营销信息
        return getCourseBaseInfo(courseId);
    }

    /**
     * 根据课程id查询课程基本信息和营销信息
     * @param courseId
     * @return
     */
    public CourseBaseInfoDto getCourseBaseInfo(long courseId){
        // 1.根据id查询课程基本信息
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        if(courseBase == null){
            return null;
        }
        // 2.根据id查询课程营销信息
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
        // 3.封装返回结果
        CourseBaseInfoDto courseBaseInfoDto = new CourseBaseInfoDto();
        // 3.1拷贝课程基本信息属性
        BeanUtils.copyProperties(courseBase, courseBaseInfoDto);
        if(courseMarket != null){
            // 3.2拷贝课程营销信息
            BeanUtils.copyProperties(courseMarket, courseBaseInfoDto);
        }
        // 4.查询大小分类名称
        CourseCategory st = courseCategoryMapper.selectById(courseBase.getSt());
        CourseCategory mt = courseCategoryMapper.selectById(courseBase.getMt());
        courseBaseInfoDto.setStName(st.getName());
        courseBaseInfoDto.setMtName(mt.getName());

        return courseBaseInfoDto;
    }
}
