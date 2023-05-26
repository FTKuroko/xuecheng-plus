package com.xuecheng.ucenter.service.impl;

import com.alibaba.fastjson.JSON;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * @author Kuroko
 * @description 自定义方法获取 UserDetails
 * @date 2023/5/26 14:57
 */
@Service
@Slf4j
public class UserServiceImpl implements UserDetailsService {
    @Autowired
    XcUserMapper xcUserMapper;
    @Autowired
    ApplicationContext applicationContext;

    /**
     * 查询用户信息组成用户身份信息
     * @param s AuthParamsDto 类型的 json 数据，可以将该方法改造成符合多种情况要求的认证入口
     * @return
     * @throws UsernameNotFoundException
     */
    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
        AuthParamsDto authParamsDto = null;
        try{
            // 将认证参数转为 AuthParamsDto 类型
            authParamsDto = JSON.parseObject(s, AuthParamsDto.class);
        }catch (Exception e){
            log.error("认证请求不符合项目要求:{]", s);
            throw new RuntimeException("认证请求数据格式不对!");
        }
        // 获取认证类型，选择对应的认证方式
        String authType = authParamsDto.getAuthType();
        AuthService authService = applicationContext.getBean(authType + "_authservice", AuthService.class);
        XcUserExt user = authService.excute(authParamsDto);

        return getUserPrincipal(user);
    }

    /**
     * 查询用户信息
     * @param user  用户 id,主键
     * @return
     */
    public UserDetails getUserPrincipal(XcUserExt user){
        // 用户权限
        String[] authorities = {"test"};
        String password = user.getPassword();
        // 令牌中不放密码
        user.setPassword(null);
        // 将 user 转为 json 对象
        String userString = JSON.toJSONString(user);
        // 封装成 UserDetails
        UserDetails userDetails = User.withUsername(userString).password(password).authorities(authorities).build();
        return userDetails;
    }
}
