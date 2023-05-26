package com.xuecheng.content.util;

import com.alibaba.fastjson.JSON;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author Kuroko
 * @description 获取当前用户身份工具类
 * @date 2023/5/26 15:25
 */
@Slf4j
public class SecurityUtil {

    public static XcUser getUser(){
        try{
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if(principal instanceof String){
                // 用户信息是用 json 存储，需要进行转换
                String jsonString = principal.toString();
                // 将 json 转对象
                XcUser xcUser = JSON.parseObject(jsonString, XcUser.class);
                return xcUser;
            }
        }catch (Exception e){
            log.error("获取当前登录用户身份出错:{}", e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    @Data
    public static class XcUser implements Serializable{
        private static final long serialVersionUID = 1L;
        private String id;
        private String username;
        private String password;
        private String salt;
        private String name;
        private String nickname;
        private String wxUnionid;
        private String companyId;
        private String userpic;
        private String utype;
        private LocalDateTime birthday;
        private String sex;
        private String email;
        private String cellphone;
        private String qq;
        private String status;
        private LocalDateTime createTime;
        private LocalDateTime updateTime;
    }
}
