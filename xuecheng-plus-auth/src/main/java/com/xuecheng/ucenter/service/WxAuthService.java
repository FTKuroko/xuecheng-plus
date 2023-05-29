package com.xuecheng.ucenter.service;

import com.xuecheng.ucenter.model.po.XcUser;

/**
 * @author Kuroko
 * @description 微信扫码接入
 * @date 2023/5/29 18:02
 */
public interface WxAuthService {
    /**
     * 微信扫码认证，申请令牌，携带令牌查询用户信息，保存用户信息到数据库
     * @param code
     * @return
     */
    XcUser wxAuth(String code);
}
