package com.xuecheng.ucenter.feignclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author Kuroko
 * @description 搜索远程服务接口
 * @date 2023/5/26 19:18
 */
@FeignClient(value = "checkcode", fallbackFactory = CheckCodeClientFactory.class)
public interface CheckCodeClient {
    @PostMapping(value = "/verify")
    public Boolean verify(@RequestParam("key") String key,@RequestParam("code") String code);

}
