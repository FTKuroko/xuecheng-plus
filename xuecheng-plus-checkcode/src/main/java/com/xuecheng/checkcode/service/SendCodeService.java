package com.xuecheng.checkcode.service;

/**
 * @author Kuroko
 * @description
 * @date 2023/5/30 14:53
 */
public interface SendCodeService {
    /**
     * 发送验证码
     * @param email 目标邮箱
     * @param code  验证码
     */
    public void sendEmail(String email, String code);
}
