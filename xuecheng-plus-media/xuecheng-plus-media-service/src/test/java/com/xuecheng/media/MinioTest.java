package com.xuecheng.media;

import io.minio.*;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.InputStream;

/**
 * @author Kuroko
 * @description 测试 MinIO
 * @date 2023/5/10 21:02
 */
public class MinioTest {
    // 建立连接
    static MinioClient minioClient = MinioClient.builder()
            .endpoint("http://192.168.101.65:9000")
            .credentials("minioadmin", "minioadmin")
            .build();

    // 上传文件
    @Test
    public void upload(){
        try{
            minioClient.uploadObject(
                    UploadObjectArgs.builder()
                            .bucket("testbucket")   // 指定存储位置
                            .object("testPic001.png")   // 存储后的对象名
                            .filename("E:\\images\\31805577-3848-4d2f-9360-031808e947ff.png")   // 本地文件位置
                            .build()
            );
            System.out.println("上传成功");
        }catch (Exception e){
            System.out.println("上传失败");
        }
    }

    // 删除文件
    @Test
    public void delete(){
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket("testbucket")
                            .object("testPic001.png")
                            .build()
            );
            System.out.println("删除成功");
        }catch (Exception e){
            System.out.println("删除失败");
        }
    }

    // 查询文件
    @Test
    public void getFile(){
        GetObjectArgs getObjectArgs = GetObjectArgs.builder().bucket("testbucket").object("testPic001.png").build();
        try(
                FilterInputStream inputStream = minioClient.getObject(getObjectArgs);
                FileOutputStream outputStream = new FileOutputStream(new File("E:\\testPic.png"));
        ) {
            IOUtils.copy(inputStream,outputStream);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
