package com.xuecheng.media.service.jobhandler;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author Kuroko
 * @description
 * @date 2023/5/12 16:32
 */
@Slf4j
@Component
public class SimpleJob {
    @XxlJob("testJob")
    public void testJob(){
        log.debug("开始执行....");
    }

    @XxlJob("shardingJobHandler")
    public void shardingJob(){
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        log.debug("shardIndex:{}, shardTotal:{}", shardIndex, shardTotal);
    }

}
