package com.xuecheng.content;

import com.xuecheng.content.config.MultipartSupportConfig;
import com.xuecheng.content.feignclient.MediaServiceClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

/**
 * @author Kuroko
 * @description
 * @date 2023/5/22 20:47
 */
@SpringBootTest
public class FeignUploadTest {
    @Autowired
    MediaServiceClient mediaServiceClient;

    // 远程调用，上传文件
    @Test
    public void test(){
        MultipartFile multipartFile = MultipartSupportConfig.getMultipartFile(new File("E:\\test.html"));
        String result = mediaServiceClient.uploadFile(multipartFile,  "course/test.html");
        System.out.println(result);
    }
}
