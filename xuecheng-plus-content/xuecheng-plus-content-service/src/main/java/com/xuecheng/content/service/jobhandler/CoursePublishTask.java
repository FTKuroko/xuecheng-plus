package com.xuecheng.content.service.jobhandler;

import com.sun.org.apache.xpath.internal.operations.Bool;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.feignclient.SearchServiceClient;
import com.xuecheng.content.mapper.CoursePublishMapper;
import com.xuecheng.content.model.po.CourseIndex;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.content.service.CoursePublishService;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MessageProcessAbstract;
import com.xuecheng.messagesdk.service.MqMessageService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * @author Kuroko
 * @description
 * @date 2023/5/22 15:03
 */
@Slf4j
@Component
public class CoursePublishTask extends MessageProcessAbstract {
    @Autowired
    CoursePublishService coursePublishService;
    @Autowired
    CoursePublishMapper coursePublishMapper;
    @Autowired
    SearchServiceClient searchServiceClient;
    // 任务调度入口
    @XxlJob("CoursePublishJobHandler")
    public void coursePublishJobHandler(){
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        process(shardIndex, shardTotal, "course_publish", 5, 60);
        log.debug("测试任务执行中...");
    }

    /**
     * 课程发布任务处理
     * @param mqMessage 执行任务内容
     * @return
     */
    @Override
    public boolean execute(MqMessage mqMessage) {
        // 获取消息的相关业务信息
        String coureseId = mqMessage.getBusinessKey1();
        log.debug("开始执行课程发布任务，课程 id 为:{}", coureseId);
        long courseId = Long.parseLong(coureseId);
        // 将课程信息静态页面上传至MinIO
        generateCourseHtml(mqMessage, courseId);
        // 课程缓存存储到Redis
        saveCourseCache(mqMessage, courseId);
        // 课程索引存储到ElasticSearch
        saveCourseIndex(mqMessage, courseId);
        return true;
    }

    /**
     * 生成课程静态化页面并上传至 MinIO
     * @param mqMessage 执行任务内容
     * @param courseId  课程 id
     */
    private void generateCourseHtml(MqMessage mqMessage, long courseId){
        log.debug("开始课程静态化,课程 id:{}", courseId);
        // 1. 幂等性判断
        // 1.1 消息 id
        Long id = mqMessage.getId();
        // 1.2 消息处理的 service
        MqMessageService mqMessageService = this.getMqMessageService();
        // 1.3 消息幂等性处理
        int stageOne = mqMessageService.getStageOne(id);
        if(stageOne > 0){
            log.debug("课程静态化已处理完毕，直接返回，课程 id:[}", courseId);
            return;
        }
        //2. 生成静态化页面
        File file = coursePublishService.generateCourseHtml(courseId);
        //3. 上传静态化页面
        if(file!=null){
            coursePublishService.uploadCourseHtml(courseId,file);
        }
        // 4.保存第一阶段状态
        mqMessageService.completedStageOne(id);
    }

    /**
     * 将课程信息写入缓存 redis
     * @param mqMessage 执行任务内容
     * @param courseId  课程 id
     */
    private void saveCourseCache(MqMessage mqMessage, long courseId){
        log.debug("将课程信息缓存至 redis，课程 id:{}", courseId);

    }

    /**
     * 保存课程索引信息
     * @param mqMessage 执行任务内容
     * @param courseId  课程 id
     */
    private void saveCourseIndex(MqMessage mqMessage, long courseId){
        log.debug("保存课程索引信息，课程 id:{}", courseId);
        // 消息 id
        Long id = mqMessage.getId();
        // 消息处理的 service
        MqMessageService mqMessageService = this.getMqMessageService();
        // 消息幂等性处理
        int stageTwo = mqMessageService.getStageTwo(id);
        if(stageTwo > 0){
            log.debug("课程索引已处理，直接返回，课程 id:{}", courseId);
            return;
        }

        // 取出课程发布信息
        CoursePublish coursePublish = coursePublishMapper.selectById(courseId);
        // 拷贝至课程索引对象
        CourseIndex courseIndex = new CourseIndex();
        BeanUtils.copyProperties(coursePublish, courseIndex);
        // 远程调用搜索服务 api 添加课程信息到索引
        Boolean add = searchServiceClient.add(courseIndex);
        if(!add){
            XueChengPlusException.cast("添加索引失败");
        }
        // 保存任务第二阶段
        mqMessageService.completedStageTwo(id);
    }
}
