package com.xuecheng.content.feignclient;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author Kuroko
 * @description
 * @date 2023/5/22 21:37
 */
@Slf4j
@Component
public class MediaServiceClientFallback implements MediaServiceClient{
    @Override
    public String uploadFile(MultipartFile upload, String objectName) {
        log.debug("方式一：熔断处理，无法获取异常");
        return null;
    }
}
