package com.xuecheng.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.media.mapper.MediaFilesMapper;
import com.xuecheng.media.mapper.MediaProcessHistoryMapper;
import com.xuecheng.media.mapper.MediaProcessMapper;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.model.po.MediaProcessHistory;
import com.xuecheng.media.service.MediaFileProcessService;
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
 * @date 2023/5/13 10:46
 */
@Service
@Slf4j
public class MediaFileProcessServiceImpl implements MediaFileProcessService {
    @Autowired
    MediaFilesMapper mediaFilesMapper;
    @Autowired
    MediaProcessMapper mediaProcessMapper;
    @Autowired
    MediaProcessHistoryMapper mediaProcessHistoryMapper;
    @Override
    public List<MediaProcess> getMediaProcessList(int shardIndex, int shardTotal, int count) {
        List<MediaProcess> mediaProcesses = mediaProcessMapper.selectListByShardIndex(shardTotal, shardIndex, count);
        return mediaProcesses;
    }

    @Override
    @Transactional
    public void saveProcessFinishStatus(Long taskId, String status, String fileId, String url, String errorMsg) {
        // 查出任务，如果不存在直接返回
        MediaProcess mediaProcess = mediaProcessMapper.selectById(taskId);
        if(mediaProcess == null){
            log.debug("更新任务状态时，此任务:{}为空", taskId);
            return;
        }
        // 处理失败，更新任务处理结果
        LambdaQueryWrapper<MediaProcess> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MediaProcess::getId, taskId);
        if(status.equals("3")){
            MediaProcess mediaProcess_new = new MediaProcess();
            mediaProcess_new.setStatus("3");
            mediaProcess_new.setErrormsg(errorMsg);
            // 更新任务失败次数
            mediaProcess_new.setFailCount(mediaProcess.getFailCount() + 1);
            // 更新时间
            mediaProcess_new.setFinishDate(LocalDateTime.now());
            mediaProcessMapper.update(mediaProcess_new, queryWrapper);
            log.debug("更新任务处理状态为失败，任务信息:{}", mediaProcess_new);
            return;
        }
        // 任务处理成功
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileId);
        if(mediaFiles != null){
            // 更新媒资文件中的访问 url
            mediaFiles.setUrl(url);
            mediaFilesMapper.updateById(mediaFiles);
        }
        // 处理成功，更新 url 和状态
        mediaProcess.setUrl(url);
        mediaProcess.setStatus("2");
        mediaProcess.setFinishDate(LocalDateTime.now());
        mediaProcessMapper.update(mediaProcess, queryWrapper);
        // 添加到历史记录
        MediaProcessHistory mediaProcessHistory = new MediaProcessHistory();
        BeanUtils.copyProperties(mediaProcess, mediaProcessHistory);
        mediaProcessHistoryMapper.insert(mediaProcessHistory);
        // 删除 mediaProcess
        mediaProcessMapper.deleteById(taskId);
    }

    @Override
    public boolean startTask(long id) {
        return mediaProcessMapper.startTask(id) <= 0 ? false : true;
    }

}
