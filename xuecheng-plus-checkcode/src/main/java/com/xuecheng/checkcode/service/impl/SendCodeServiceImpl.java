package com.xuecheng.checkcode.service.impl;

import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.checkcode.service.SendCodeService;
import com.xuecheng.checkcode.utils.MailUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import java.util.concurrent.TimeUnit;

/**
 * @author Kuroko
 * @description
 * @date 2023/5/30 14:54
 */
@Slf4j
@Service
public class SendCodeServiceImpl implements SendCodeService {
    // 设置验证码过期时间
    public final Long CODE_TTL = 120L;
    // 验证码存入 redis
    @Autowired
    StringRedisTemplate redisTemplate;

    /**
     * 发送验证码
     * @param email 目标邮箱
     * @param code  验证码
     */
    @Override
    public void sendEmail(String email, String code) {
        // 1. 向用户发送验证码
        try{
            MailUtil.sendTestMail(email, code);
        }catch (MessagingException e){
            log.debug("邮件发送失败:{}", e.getMessage());
            XueChengPlusException.cast("发送验证码失败!");
        }
        // 2. 将验证码放入缓存,过期时间为两分钟
        redisTemplate.opsForValue().set(email, code, CODE_TTL, TimeUnit.SECONDS);
    }
}
