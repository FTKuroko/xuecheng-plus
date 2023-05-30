package com.xuecheng.ucenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.mapper.XcUserRoleMapper;
import com.xuecheng.ucenter.model.dto.FindPswDto;
import com.xuecheng.ucenter.model.dto.RegisterDto;
import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.model.po.XcUserRole;
import com.xuecheng.ucenter.service.VerifyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @author Kuroko
 * @description
 * @date 2023/5/30 14:47
 */
@Slf4j
@Service
public class VerifyServiceImpl implements VerifyService {
    @Autowired
    StringRedisTemplate redisTemplate;
    @Autowired
    XcUserMapper xcUserMapper;
    @Autowired
    XcUserRoleMapper xcUserRoleMapper;
    /**
     * 找回密码
     * @param findPswDto    新密码数据
     */
    @Override
    @Transactional
    public void findPassword(FindPswDto findPswDto) {
        String email = findPswDto.getEmail();
        String checkcode = findPswDto.getCheckcode();
        // 1、校验验证码，不一致则抛出异常
        boolean verify = verify(email, checkcode);
        if(!verify){
            throw new RuntimeException("验证码输入错误!");
        }
        // 2、判断两次密码是否一致，不一致则抛出异常
        String password = findPswDto.getPassword();
        String confirmpwd = findPswDto.getConfirmpwd();
        if(!password.equals(confirmpwd)){
            throw new RuntimeException("两次输入的密码不一致!");
        }
        // 3、根据手机号和邮箱查询用户
        XcUser xcUser = xcUserMapper.selectOne(new LambdaQueryWrapper<XcUser>().eq(XcUser::getEmail, email));
        if(xcUser == null){
            throw new RuntimeException("用户不存在!");
        }
        // 4、如果找到用户更新为新密码。要对密码进行加密
        xcUser.setPassword(new BCryptPasswordEncoder().encode(password));
        xcUserMapper.updateById(xcUser);
    }

    @Override
    @Transactional
    public void register(RegisterDto registerDto) {
        // 随机生成一个 UUID
        String uuid = UUID.randomUUID().toString();
        String email = registerDto.getEmail();
        String checkcode = registerDto.getCheckcode();
        // 校验验证码，不一致，抛异常
        if(!verify(email, checkcode)){
            throw new RuntimeException("验证码输入错误!");
        }
        String password = registerDto.getPassword();
        String confirmpwd = registerDto.getConfirmpwd();
        // 校验两次密码是否一致，不一致，抛异常
        if(!password.equals(confirmpwd)){
            throw new RuntimeException("两次输入密码不一致!");
        }
        // 校验用户是否存在，已存在，抛异常
        XcUser xcUser = xcUserMapper.selectOne(new LambdaQueryWrapper<XcUser>().eq(XcUser::getEmail, email));
        if(xcUser != null){
            throw new RuntimeException("用户已存在，一个邮箱只能注册一个账号!");
        }

        // 向用户表、用户关系角色表添加数据，角色为学生
        XcUser user = new XcUser();
        BeanUtils.copyProperties(registerDto, user);
        user.setId(uuid);
        user.setPassword(new BCryptPasswordEncoder().encode(password));
        user.setUtype("101001");  // 学生类型
        user.setStatus("1");
        user.setName(registerDto.getNickname());
        user.setCreateTime(LocalDateTime.now());
        int insert = xcUserMapper.insert(user);
        if(insert <= 0){
            throw new RuntimeException("新增用户信息失败!");
        }

        XcUserRole xcUserRole = new XcUserRole();
        xcUserRole.setId(uuid);
        xcUserRole.setUserId(uuid);
        xcUserRole.setRoleId("17");
        xcUserRole.setCreateTime(LocalDateTime.now());
        int insert1 = xcUserRoleMapper.insert(xcUserRole);
        if(insert1 <= 0){
            throw new RuntimeException("新增用户角色信息失败!");
        }
    }

    /**
     * 校验验证码
     * @param email     邮箱
     * @param checkcode 用户输入的验证码
     * @return
     */
    public Boolean verify(String email, String checkcode){
        // 从缓存中获取验证码
        String code = redisTemplate.opsForValue().get(email);
        // 校验验证码
        if(code.equalsIgnoreCase(checkcode)){
            redisTemplate.delete(email);
            return true;
        }
        return false;
    }
}
