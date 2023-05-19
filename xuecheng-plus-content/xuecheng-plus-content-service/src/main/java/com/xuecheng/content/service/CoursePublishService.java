package com.xuecheng.content.service;

import com.xuecheng.content.model.dto.CoursePreviewDto;

/**
 * @author Kuroko
 * @description 课程预览、发布接口
 * @date 2023/5/18 16:13
 */
public interface CoursePublishService {
    /**
     * 获取课程预览信息
     * @param courseId 课程 id
     * @return
     */
    public CoursePreviewDto getCoursePreviewInfo(Long courseId);

    /**
     * 提交课程审核
     * @param companyId 机构 id
     * @param courseId  课程 id
     */
    public void commitAudit(Long companyId, Long courseId);
}
