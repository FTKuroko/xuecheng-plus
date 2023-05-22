package com.xuecheng.content.feignclient;

import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author Kuroko
 * @description
 * @date 2023/5/22 21:39
 */
@Slf4j
@Component
public class MediaServiceClientFallbackFactory implements FallbackFactory<MediaServiceClient> {

    @Override
    public MediaServiceClient create(Throwable throwable) {
        return new MediaServiceClient() {
            @Override
            public String uploadFile(MultipartFile upload, String objectName) {
                log.debug("方式二：熔断处理，熔断异常：{}", throwable.getMessage());
                return null;
            }
        };
    }
}
