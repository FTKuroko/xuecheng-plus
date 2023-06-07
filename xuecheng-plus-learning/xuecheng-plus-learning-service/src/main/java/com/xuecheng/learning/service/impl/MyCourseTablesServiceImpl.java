package com.xuecheng.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.learning.feignclient.ContentServiceClient;
import com.xuecheng.learning.mapper.XcChooseCourseMapper;
import com.xuecheng.learning.mapper.XcCourseTablesMapper;
import com.xuecheng.learning.model.dto.MyCourseTableParams;
import com.xuecheng.learning.model.dto.XcChooseCourseDto;
import com.xuecheng.learning.model.dto.XcCourseTablesDto;
import com.xuecheng.learning.model.po.XcChooseCourse;
import com.xuecheng.learning.model.po.XcCourseTables;
import com.xuecheng.learning.service.MyCourseTablesService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author Kuroko
 * @description
 * @date 2023/5/31 15:25
 */
@Service
@Slf4j
public class MyCourseTablesServiceImpl implements MyCourseTablesService {
    @Autowired
    XcChooseCourseMapper xcChooseCourseMapper;  // 操作选课记录
    @Autowired
    XcCourseTablesMapper xcCourseTablesMapper;  // 操作用户课程表
    @Autowired
    ContentServiceClient contentServiceClient;  // 远程调用

    /**
     * 添加选课
     * @param userId    用户 id
     * @param courseId  课程 id
     * @return
     */
    @Override
    @Transactional
    public XcChooseCourseDto addChooseCourse(String userId, Long courseId) {
        // 1. 查询课程信息,远程调用查询课程发布信息
        CoursePublish coursepublish = contentServiceClient.getCoursepublish(courseId);
        // 2. 课程收费标准
        String charge = coursepublish.getCharge();
        // 选课记录
        XcChooseCourse xcChooseCourse = new XcChooseCourse();
        if("201000".equals(charge)){
            // 2.1 免费课程
            // 添加选课记录
            xcChooseCourse = addFreeCourse(userId, coursepublish);
            // 添加我的课程表
            XcCourseTables xcCourseTables = addCourseTables(xcChooseCourse);
        }else{
            // 2.2 收费课程
            // 添加选课记录
            xcChooseCourse = addChargeCourse(userId, coursepublish);
        }
        // 3. 获取学习资格
        XcCourseTablesDto xcCourseTablesDto = getLearningStatus(userId, courseId);
        XcChooseCourseDto xcChooseCourseDto = new XcChooseCourseDto();
        BeanUtils.copyProperties(xcChooseCourse, xcChooseCourseDto);
        xcChooseCourseDto.setLearnStatus(xcCourseTablesDto.getLearnStatus());

        return xcChooseCourseDto;
    }

    /**
     * 将免费课程添加到学科记录表
     * @param userId        用户 id
     * @param coursePublish 已发布的课程
     * @return
     */
    public XcChooseCourse addFreeCourse(String userId, CoursePublish coursePublish){
        // 先查询选课记录表，判断是否已经选择过该门课程，因为用户选课时是可以多次点击选课按钮的，每点一次都是选一次课
        LambdaQueryWrapper<XcChooseCourse> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(XcChooseCourse::getUserId, userId)
                .eq(XcChooseCourse::getCourseId, coursePublish.getId())
                .eq(XcChooseCourse::getOrderType, "700001") // 选课类型:免费类型
                .eq(XcChooseCourse::getStatus, "701001");   // 选课状态:选课成功
        List<XcChooseCourse> xcChooseCourses = xcChooseCourseMapper.selectList(queryWrapper);
        if(xcChooseCourses != null && xcChooseCourses.size() > 0){
            // 已经选择过
            return xcChooseCourses.get(0);
        }

        // 没有选过，需要添加到选课记录表
        XcChooseCourse xcChooseCourse = new XcChooseCourse();
        xcChooseCourse.setCourseId(coursePublish.getId());
        xcChooseCourse.setCourseName(coursePublish.getName());
        xcChooseCourse.setCoursePrice(0f);  // 免费课程
        xcChooseCourse.setCompanyId(coursePublish.getCompanyId());
        xcChooseCourse.setUserId(userId);
        xcChooseCourse.setOrderType("700001");
        xcChooseCourse.setValidDays(365);   // 默认有效期为一年
        xcChooseCourse.setCreateDate(LocalDateTime.now());
        xcChooseCourse.setStatus("701001");
        xcChooseCourse.setValidtimeStart(LocalDateTime.now());
        xcChooseCourse.setValidtimeEnd(LocalDateTime.now().plusDays(365));

        int insert = xcChooseCourseMapper.insert(xcChooseCourse);
        if(insert <= 0){
            XueChengPlusException.cast("添加选课失败!");
        }

        return xcChooseCourse;
    }

    /**
     * 添加到我的课程表
     * @param xcChooseCourse    选课记录
     * @return
     */
    public XcCourseTables addCourseTables(XcChooseCourse xcChooseCourse){
        // 选课记录完成且未过期即可添加到我的课程表
        String status = xcChooseCourse.getStatus();
        if(!"701001".equals(status)){
            XueChengPlusException.cast("选课未成功,无法添加到课程表!");
        }
        // 查询我的课程表，如果该课程已经选过则直接返回
        XcCourseTables courseTables = getCourseTables(xcChooseCourse.getUserId(), xcChooseCourse.getCourseId());
        if(courseTables != null){
            return courseTables;
        }

        // 创建一个课程表，封装数据
        XcCourseTables xcCourseTables = new XcCourseTables();
        BeanUtils.copyProperties(xcChooseCourse, xcCourseTables);
        xcCourseTables.setChooseCourseId(xcChooseCourse.getId());
        xcCourseTables.setCourseType(xcChooseCourse.getOrderType());
        xcCourseTables.setUpdateDate(LocalDateTime.now());
        // 添加到课程表数据库
        int insert = xcCourseTablesMapper.insert(xcCourseTables);
        if(insert <= 0){
            XueChengPlusException.cast("添加到课程表失败!");
        }
        return xcCourseTables;
    }

    /**
     * 查询用户选择的指定课程
     * @param userId    用户 id
     * @param courseId  课程 id
     * @return
     */
    public XcCourseTables getCourseTables(String userId, Long courseId){
        LambdaQueryWrapper<XcCourseTables> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(XcCourseTables::getCourseId, courseId)
                .eq(XcCourseTables::getUserId, userId);
        XcCourseTables xcCourseTables = xcCourseTablesMapper.selectOne(queryWrapper);
        return xcCourseTables;
    }

    /**
     * 将付费课程添加到选课记录表
     * @param userId        用户 id
     * @param coursePublish 已发布课程
     * @return
     */
    public XcChooseCourse addChargeCourse(String userId, CoursePublish coursePublish){
        // 如果存在添加记录则直接返回
        LambdaQueryWrapper<XcChooseCourse> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(XcChooseCourse::getUserId, userId)
                .eq(XcChooseCourse::getCourseId, coursePublish.getId())
                .eq(XcChooseCourse::getOrderType, "700002")     // 收费订单
                .eq(XcChooseCourse::getStatus, "701002");        // 待支付，收费课程存入课程记录表的初始状态都是待支付，支付成功后再修改成已支付
        List<XcChooseCourse> xcChooseCourses = xcChooseCourseMapper.selectList(queryWrapper);
        if(xcChooseCourses != null && xcChooseCourses.size() > 0){
            return xcChooseCourses.get(0);
        }
        // 不存在添加记录
        XcChooseCourse xcChooseCourse = new XcChooseCourse();
        xcChooseCourse.setCourseId(coursePublish.getId());
        xcChooseCourse.setCourseName(coursePublish.getName());
        xcChooseCourse.setCoursePrice(coursePublish.getPrice());  // 免费课程
        xcChooseCourse.setCompanyId(coursePublish.getCompanyId());
        xcChooseCourse.setUserId(userId);
        xcChooseCourse.setOrderType("700002");
        xcChooseCourse.setValidDays(coursePublish.getValidDays());
        xcChooseCourse.setCreateDate(LocalDateTime.now());
        xcChooseCourse.setStatus("701002"); // 待支付
        xcChooseCourse.setValidtimeStart(LocalDateTime.now());
        xcChooseCourse.setValidtimeEnd(LocalDateTime.now().plusDays(coursePublish.getValidDays()));

        int insert = xcChooseCourseMapper.insert(xcChooseCourse);
        if(insert <= 0){
            XueChengPlusException.cast("添加收费课程失败!");
        }
        return xcChooseCourse;
    }

    /**
     * 判断学习资格
     * @param userId    用户 id
     * @param courseId  课程 id
     * @return  XcCourseTablesDto
     * 学习资格状态 [{"code":"702001","desc":"正常学习"},{"code":"702002","desc":"没有选课或选课后没有支付"},{"code":"702003","desc":"已过期需要申请续期或重新支付"}]
     */
    @Override
    public XcCourseTablesDto getLearningStatus(String userId, Long courseId) {
        // 查询我的课程表
        XcCourseTables courseTables = getCourseTables(userId, courseId);
        if(courseTables == null){
            XcCourseTablesDto xcCourseTablesDto = new XcCourseTablesDto();
            // 没有选课或者选课后还没有支付
            xcCourseTablesDto.setLearnStatus("702002");
            return xcCourseTablesDto;
        }
        XcCourseTablesDto xcCourseTablesDto = new XcCourseTablesDto();
        BeanUtils.copyProperties(courseTables, xcCourseTablesDto);
        // 是否过期， true 为过期， false 为未过期
        boolean isExpires = courseTables.getValidtimeEnd().isBefore(LocalDateTime.now());
        if(!isExpires){
            // 未过期,正常学习
            xcCourseTablesDto.setLearnStatus("702001");
        }else{
            // 已过期
            xcCourseTablesDto.setLearnStatus("702003");
        }
        return xcCourseTablesDto;
    }

    /**
     * 保存选课成功状态
     * @param chooseCourseId    选课 id
     * @return
     */
    @Override
    @Transactional
    public boolean saveChooseCourseStatus(String chooseCourseId) {
        // 1. 根据选课id，查询选课表
        XcChooseCourse chooseCourse = xcChooseCourseMapper.selectById(chooseCourseId);
        if (chooseCourse == null) {
            log.error("接收到购买课程的消息，根据选课id未查询到课程，选课id：{}", chooseCourseId);
            return false;
        }
        // 2. 选课状态为未支付时，更新选课状态为选课成功
        if ("701002".equals(chooseCourse.getStatus())) {
            chooseCourse.setStatus("701001");
            int update = xcChooseCourseMapper.updateById(chooseCourse);
            if (update <= 0) {
                log.error("更新选课记录失败：{}", chooseCourse);
            }
        }
        // 3. 向我的课程表添加记录
        addCourseTables(chooseCourse);
        return true;
    }

    @Override
    public PageResult<XcCourseTables> mycourestabls(MyCourseTableParams params) {
        // 页码
        int pageNo = params.getPage();
        // 每页记录数，固定为 4
        int pageSize = params.getSize();
        // 分页条件
        Page<XcCourseTables> page = new Page<>(pageNo, pageSize);
        // 根据用户 id 查询课程表
        String userId = params.getUserId();
        LambdaQueryWrapper<XcCourseTables> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(XcCourseTables::getUserId, userId);
        // 分页查询
        Page<XcCourseTables> xcCourseTablesPage = xcCourseTablesMapper.selectPage(page, queryWrapper);
        List<XcCourseTables> records = xcCourseTablesPage.getRecords();
        // 总记录数
        long total = xcCourseTablesPage.getTotal();
        PageResult<XcCourseTables> pageResult = new PageResult<>(records, total, pageNo, pageSize);
        return pageResult;
    }
}
