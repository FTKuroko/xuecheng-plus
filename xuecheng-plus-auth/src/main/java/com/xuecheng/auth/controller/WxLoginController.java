package com.xuecheng.auth.controller;

import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.service.impl.WxAuthServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;

/**
 * @author Kuroko
 * @description 微信登录接口
 * @date 2023/5/29 10:35
 */
@Slf4j
@Controller
public class WxLoginController {
    @Autowired
    WxAuthServiceImpl wxAuthService;

    @RequestMapping("/wxLogin")
    public String wxLogin(String code, String state) throws IOException {
        log.debug("微信扫码回调, code:{}, state:{}", code, state);
        // 请求微信申请令牌，拿到令牌查询用户信息，将用户信息写入本项目数据库
        XcUser xcUser = wxAuthService.wxAuth(code);
        if(xcUser == null){
            // 用户不存在，重定向到错误界面
            return "redirect:http://localhost/error.html";
        }
        // 用户存在
        String username = xcUser.getUsername();
        return "redirect:http://localhost/sign.html?username=" + username + "&authType=wx";
    }
}
