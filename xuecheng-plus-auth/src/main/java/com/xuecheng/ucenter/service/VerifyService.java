package com.xuecheng.ucenter.service;

import com.xuecheng.ucenter.model.dto.FindPswDto;
import com.xuecheng.ucenter.model.dto.RegisterDto;

/**
 * @author Kuroko
 * @description
 * @date 2023/5/30 14:46
 */
public interface VerifyService {

    /**
     * 找回密码
     * @param findPswDto    新密码数据
     */
    void findPassword(FindPswDto findPswDto);

    /**
     * 用户注册
     * @param registerDto   用户注册数据
     */
    void register(RegisterDto registerDto);
}
