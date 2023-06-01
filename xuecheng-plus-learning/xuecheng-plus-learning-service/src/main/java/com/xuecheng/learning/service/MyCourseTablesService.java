package com.xuecheng.learning.service;

import com.xuecheng.learning.model.dto.XcChooseCourseDto;
import com.xuecheng.learning.model.dto.XcCourseTablesDto;

/**
 * @author Kuroko
 * @description 我的课程表 service 接口
 * @date 2023/5/31 15:24
 */
public interface MyCourseTablesService {
    /**
     * 添加选课
     * @param userId    用户 id
     * @param courseId  课程 id
     * @return
     */
    public XcChooseCourseDto addChooseCourse(String userId, Long courseId);

    /**
     * 判断学习资格
     * @param userId    用户 id
     * @param courseId  课程 id
     * @return XcCourseTablesDto
     * 学习资格状态 [{"code":"702001","desc":"正常学习"},{"code":"702002","desc":"没有选课或选课后没有支付"},{"code":"702003","desc":"已过期需要申请续期或重新支付"}]
     */
    public XcCourseTablesDto getLearningStatus(String userId, Long courseId);
}
