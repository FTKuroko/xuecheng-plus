package com.xuecheng.ucenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.service.AuthService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * @author Kuroko
 * @description 账号密码认证方式
 * @date 2023/5/26 17:07
 */
@Service("password_authservice")
public class PasswordAuthServiceImpl implements AuthService {
    @Autowired
    XcUserMapper xcUserMapper;
    @Autowired
    PasswordEncoder passwordEncoder;

    @Override
    public XcUserExt excute(AuthParamsDto authParamsDto) {
        // 账号
        String username = authParamsDto.getUsername();
        XcUser user = xcUserMapper.selectOne(new LambdaQueryWrapper<XcUser>().eq(XcUser::getUsername, username));
        if(user == null){
            throw new RuntimeException("账号不存在!");
        }
        XcUserExt xcUserExt = new XcUserExt();
        BeanUtils.copyProperties(user, xcUserExt);
        // 校验密码
        String passwordDb = user.getPassword(); // 数据库中的密码
        String passwordForm = authParamsDto.getPassword();  // 表单填写的密码
        boolean matches = passwordEncoder.matches(passwordForm, passwordDb);    // 表单密码是用户输入的，是明文形式，数据库中的密码是经过加密的，需要注入解码器进行密码校验
        if(!matches){
            throw new RuntimeException("账号或密码错误!");
        }
        // 校验通过
        return xcUserExt;
    }
}
