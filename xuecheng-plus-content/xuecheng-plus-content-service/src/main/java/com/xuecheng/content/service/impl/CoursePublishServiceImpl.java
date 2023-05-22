package com.xuecheng.content.service.impl;

import com.alibaba.fastjson.JSON;
import com.xuecheng.base.exception.CommonError;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.mapper.CourseMarketMapper;
import com.xuecheng.content.mapper.CoursePublishMapper;
import com.xuecheng.content.mapper.CoursePublishPreMapper;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.model.po.CourseMarket;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.content.model.po.CoursePublishPre;
import com.xuecheng.content.service.CourseBaseInfoService;
import com.xuecheng.content.service.CoursePublishService;
import com.xuecheng.content.service.TeachplanService;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MqMessageService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author Kuroko
 * @description
 * @date 2023/5/18 16:14
 */
@Slf4j
@Service
public class CoursePublishServiceImpl implements CoursePublishService {
    @Autowired
    CourseBaseInfoService courseBaseInfoService;
    @Autowired
    TeachplanService teachplanService;
    @Autowired
    CourseBaseMapper courseBaseMapper;
    @Autowired
    CourseMarketMapper courseMarketMapper;
    @Autowired
    CoursePublishPreMapper coursePublishPreMapper;
    @Autowired
    CoursePublishMapper coursePublishMapper;
    @Autowired
    MqMessageService mqMessageService;
    /**
     * 获取课程预览信息
     * @param courseId 课程 id
     * @return
     */
    @Override
    public CoursePreviewDto getCoursePreviewInfo(Long courseId) {
        // 获取课程基本信息及课程营销信息
        CourseBaseInfoDto courseBaseInfo = courseBaseInfoService.getCourseBaseInfo(courseId);
        // 获取课程计划信息
        List<TeachplanDto> teachplanTree = teachplanService.findTeachplanTree(courseId);
        // 数据封装
        CoursePreviewDto coursePreviewDto = new CoursePreviewDto();
        coursePreviewDto.setCourseBase(courseBaseInfo);
        coursePreviewDto.setTeachplans(teachplanTree);
        return coursePreviewDto;
    }

    /**
     * 提交课程审核
     * @param companyId 机构 id
     * @param courseId  课程 id
     */
    @Override
    @Transactional
    public void commitAudit(Long companyId, Long courseId) {
        // 课程基本信息
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        // 课程营销信息
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
        // 课程基本信息加部分营销信息
        CourseBaseInfoDto courseBaseInfo = courseBaseInfoService.getCourseBaseInfo(courseId);
        // 课程计划信息
        List<TeachplanDto> teachplanTree = teachplanService.findTeachplanTree(courseId);

        // 1. 约束校验
        // 1.1 当前审核状态为已提交不允许再次提交
        String auditStatus = courseBase.getAuditStatus();
        if("202003".equals(auditStatus)){
            XueChengPlusException.cast("当前课程正在审核，审核完成后可再次提交!");
        }
        // 1.2 本机构只允许提交本机构的课程
        if(!companyId.equals(courseBase.getCompanyId())){
            XueChengPlusException.cast("不允许提交其他机构的课程!");
        }
        // 1.3 课程图片是否填写
        if(StringUtils.isEmpty(courseBase.getPic())){
            XueChengPlusException.cast("提交失败，请先上传课程图片!");
        }
        // 1.4 课程计划是否添加
        if(teachplanTree.isEmpty() || teachplanTree == null){
            XueChengPlusException.cast("提交失败，请先填写课程计划!");
        }

        // 2. 整合课程预发布信息。
        CoursePublishPre coursePublishPre = new CoursePublishPre();
        BeanUtils.copyProperties(courseBaseInfo, coursePublishPre);
        // 课程营销信息 json 格式
        coursePublishPre.setMarket(JSON.toJSONString(courseMarket));
        // 课程计划信息 json 格式
        coursePublishPre.setTeachplan(JSON.toJSONString(teachplanTree));
        coursePublishPre.setCompanyId(companyId);
        coursePublishPre.setCreateDate(LocalDateTime.now());

        // 3. 设置预发布记录状态，已提交
        coursePublishPre.setStatus("202003");

        // 4. 添加课程预发布记录，如果之前已经提交过该门课程，则为更新操作
        CoursePublishPre coursePublishPreUpdate = coursePublishPreMapper.selectById(courseId);
        if(coursePublishPreUpdate == null){
            // 新增
            coursePublishPreMapper.insert(coursePublishPre);
        }else{
            // 更新
            coursePublishPreMapper.updateById(coursePublishPre);
        }
        // 5. 更新课程基本表审核状态
        courseBase.setAuditStatus("202003");
        courseBaseMapper.updateById(courseBase);
    }

    /**
     * 课程发布
     * @param companyId 机构 id
     * @param courseId  课程 id
     */
    @Override
    @Transactional
    public void publish(Long companyId, Long courseId) {
        // 约束校验
        // 1.1 课程审核通过方可发布
        CoursePublishPre coursePublishPre = coursePublishPreMapper.selectById(courseId);
        if(coursePublishPre == null){
            XueChengPlusException.cast("请先提交课程审核!");
        }
        // 审核状态
        String status = coursePublishPre.getStatus();
        if(!"202004".equals(status)){
            XueChengPlusException.cast("课程审核通过后才能发布!");
        }
        // 1.2 本机构只允许发布本机构的课程
        if(!companyId.equals(coursePublishPre.getCompanyId())){
            XueChengPlusException.cast("不允许提交其他机构课程!");
        }
        // 2.向课程发布表course_publish插入一条记录,记录来源于课程预发布表，如果存在则更新，发布状态为：已发布。
        saveCoursePublish(courseId);
        // 3. 删除课程预发布表的对应记录
        coursePublishPreMapper.deleteById(courseId);
        // 4. 向mq_message消息表插入一条消息，消息类型为：course_publish
        saveCoursePublishMessage(courseId);
    }

    /**
     * 保存课程发布信息
     * @param courseId  课程 id
     */
    private void saveCoursePublish(Long courseId){
        // 整合课程发布信息
        // 查询课程预发布表
        CoursePublishPre coursePublishPre = coursePublishPreMapper.selectById(courseId);
        // 课程发布表对象
        CoursePublish coursePublish = new CoursePublish();

        // 属性拷贝
        BeanUtils.copyProperties(coursePublishPre, coursePublish);
        // 更新课程发布表发布状态
        coursePublish.setStatus("203002");
        // 如果课程发布表中已经含有该课程则是更新操作
        CoursePublish coursePublishUpdate = coursePublishMapper.selectById(courseId);
        if(coursePublishUpdate == null){
            // 新增
            coursePublishMapper.insert(coursePublish);
        }else {
            // 更新
            coursePublishMapper.updateById(coursePublish);
        }
        // 更新课程基本信息表中的课程发布状态
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        courseBase.setStatus("203002");
        courseBaseMapper.updateById(courseBase);
    }

    /**
     * 保存消息表
     * @param courseId 课程 id
     */
    private void saveCoursePublishMessage(Long courseId){
        MqMessage message = mqMessageService.addMessage("course_publish", String.valueOf(courseId), null, null);
        if(message == null){
            XueChengPlusException.cast(CommonError.UNKOWN_ERROR);
        }
    }
}
