package com.xuecheng.content.service;

import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.model.po.CoursePublish;

import java.io.File;

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

    /**
     * 课程发布
     * @param companyId 机构 id
     * @param courseId  课程 id
     */
    public void publish(Long companyId, Long courseId);

    /**
     * 课程静态化
     * @param courseId  课程 id
     * @return
     */
    public File generateCourseHtml(Long courseId);

    /**
     * 上传课程静态化页面
     * @param courseId  课程 id
     * @param file      静态化文件
     */
    public void uploadCourseHtml(Long courseId, File file);

    /**
     * 查询课程发布信息
     * @param courseId  课程 id
     * @return
     */
    public CoursePublish getCoursePublish(Long courseId);
}
