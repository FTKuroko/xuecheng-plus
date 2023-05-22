package com.xuecheng;

import com.spring4all.swagger.EnableSwagger2Doc;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * @author Kuroko
 * @description
 * @date 2023/5/6 11:03
 */
@SpringBootApplication(scanBasePackages = "com.xuecheng")
@EnableSwagger2Doc
@EnableFeignClients(basePackages={"com.xuecheng.content.feignclient"})
public class ContentApplication {
    public static void main(String[] args) {
        SpringApplication.run(ContentApplication.class, args);
    }
}
