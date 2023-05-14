package com.xuecheng.media.service;

import com.xuecheng.media.model.po.MediaProcess;

import java.util.List;

/**
 * @author Kuroko
 * @description 媒资文件处理业务方法
 * @date 2023/5/13 10:45
 */
public interface MediaFileProcessService {
    /**
     * 获取待处理任务
     * @param shardIndex 分片序号
     * @param shardTotal 分片总数
     * @param count 获取记录数
     * @return
     */
    public List<MediaProcess> getMediaProcessList(int shardIndex, int shardTotal, int count);

    /**
     * 保存任务结果
     * @param taskId 任务 id
     * @param status 任务状态
     * @param fileId 文件 id
     * @param url    url
     * @param errorMsg 错误信息
     */
    public void saveProcessFinishStatus(Long taskId,String status,String fileId,String url,String errorMsg);

    /**
     * 开启一个任务
     * @param id 任务 id
     * @return
     */
    public boolean startTask(long id);
}
