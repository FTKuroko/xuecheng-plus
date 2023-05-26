package com.xuecheng.ucenter.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.model.po.XcUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import springfox.documentation.spring.web.json.Json;

/**
 * @author Kuroko
 * @description 自定义方法获取 UserDetails
 * @date 2023/5/26 14:57
 */
@Service
public class UserServiceImpl implements UserDetailsService {
    @Autowired
    XcUserMapper xcUserMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 根据 username 账号查询数据库
        XcUser xcUser = xcUserMapper.selectOne(new LambdaQueryWrapper<XcUser>().eq(XcUser::getUsername, username));
        // 查询用户不存在
        if(xcUser == null){
            return null;
        }
        // 查到了用户，取出正确密码，方便后续比对
        String password = xcUser.getPassword();
        // 将查询到的用户信息封装成 UserDetails 对象返回
        // withUsername()参数传的是什么，返回的就是什么信息，如果想要扩展用户信息内容，可以存入 json 数据作为 username 内容
        // 转 json 前需要先将敏感信息屏蔽，避免返回给前端
        xcUser.setPassword(null);
        String jsonString = JSON.toJSONString(xcUser);
        UserDetails userDetails = User.withUsername(jsonString).password(password).authorities("test").build();

        return userDetails;
    }
}
