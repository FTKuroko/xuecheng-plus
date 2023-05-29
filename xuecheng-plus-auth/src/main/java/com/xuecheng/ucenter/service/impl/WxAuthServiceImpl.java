package com.xuecheng.ucenter.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.mapper.XcUserRoleMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.model.po.XcUserRole;
import com.xuecheng.ucenter.service.AuthService;
import com.xuecheng.ucenter.service.WxAuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * @author Kuroko
 * @description 微信扫码认证方式
 * @date 2023/5/26 17:08
 */
@Slf4j
@Service("wx_authservice")
public class WxAuthServiceImpl implements AuthService, WxAuthService {
    @Autowired
    XcUserMapper xcUserMapper;
    @Autowired
    RestTemplate restTemplate;
    @Autowired
    XcUserRoleMapper xcUserRoleMapper;
    @Autowired
    WxAuthServiceImpl wxAuthService;
    @Value("${weixin.appid}")
    String appid;
    @Value("${weixin.secret}")
    String secret;

    @Override
    public XcUser wxAuth(String code){
        // 申请令牌
        Map<String, String> accessToken_map = getAccess_token(code);
        if(accessToken_map == null){
            throw new RuntimeException("申请令牌失败!");
        }
        // 获取用户信息
        String accessToken = accessToken_map.get("access_token");
        String openid = accessToken_map.get("openid");
        Map<String, String> userInfo_map = getUserInfo(accessToken, openid);
        if(userInfo_map == null){
            throw new RuntimeException("获取用户信息失败!");
        }
        // 添加用户信息到数据库
        XcUser xcUser = wxAuthService.addWxUser(userInfo_map);
        return xcUser;
    }

    /**
     * 微信扫码认证,不需要校验密码和验证码
     * @param authParamsDto 认证参数
     * @return
     */
    @Override
    public XcUserExt excute(AuthParamsDto authParamsDto) {
        // 获取账号
        String username = authParamsDto.getUsername();
        XcUser xcUser = xcUserMapper.selectOne(new LambdaQueryWrapper<XcUser>().eq(XcUser::getUsername, username));
        // 账号不存在
        if(xcUser == null){
            throw new RuntimeException("账号不存在!");
        }
        // 账号存在,封装信息返回
        XcUserExt xcUserExt = new XcUserExt();
        BeanUtils.copyProperties(xcUser, xcUserExt);
        return xcUserExt;
    }

    /**
     * 申请访问令牌，响应为 json 数据类型，用 map 存储方便取出
     * @param code
     * @return
     */
    private Map<String, String> getAccess_token(String code){
        // 微信响应的请求路径模板，之后用数据填充
        String url_template = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=%s&secret=%s&code=%s&grant_type=authorization_code";
        // 数据填充后的真实 url
        String url = String.format(url_template, appid, secret, code);
        log.debug("调用微信接口申请令牌， url:{}", url);

        // 第三方远程调用
        ResponseEntity<String> exchange = restTemplate.exchange(url, HttpMethod.POST, null, String.class);
        String body = exchange.getBody();
        // body 是 json 数据
        log.debug("调用微信接口申请令牌， 返回令牌:{}", body);
        Map<String, String> resultMap = JSON.parseObject(body, Map.class);

        return resultMap;
    }

    /**
     * 携带令牌取获取用户信息
     * http请求方式: GET
     * https://api.weixin.qq.com/sns/userinfo?access_token=ACCESS_TOKEN&openid=OPENID
     * 响应的结果同样是 json 数据形式
     * @param access_token  令牌
     * @param openid        普通用户标识
     * @return
     */
    private Map<String, String> getUserInfo(String access_token, String openid){
        // 请求模板路径
        String url_template = "https://api.weixin.qq.com/sns/userinfo?access_token=%s&openid=%s";
        // 请求真实路径
        String url = String.format(url_template, access_token, openid);
        log.debug("携带令牌获取用户信息， url:{}", url);

        // 第三方远程调用
        ResponseEntity<String> exchange = restTemplate.exchange(url, HttpMethod.GET, null, String.class);
        String body = exchange.getBody();
        // 防止乱码，进行转码
        String result = new String(body.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
        log.debug("携带令牌获取用户信息， 用户信息:{}", result);
        Map<String, String> resultMap = JSON.parseObject(result, Map.class);

        return resultMap;
    }

    /**
     * 保存用户信息到数据库
     * @param userInfo_map
     * @return
     */
    @Transactional
    public XcUser addWxUser(Map<String, String> userInfo_map){
        String unionid = userInfo_map.get("unionid").toString();
        // 根据 unionid 查询当前用户是否已经存在
        XcUser xcUser = xcUserMapper.selectOne(new LambdaQueryWrapper<XcUser>().eq(XcUser::getWxUnionid, unionid));
        if(xcUser!=null){
            return xcUser;
        }
        // 用户不存在，则需要将用户信息保存到数据库中，有两个表需要操作，一个是用户信息表，一个是用户角色关系表
        String userId = UUID.randomUUID().toString();   // 数据库中的 userId 是 UUID 形式
        xcUser = new XcUser();
        xcUser.setId(userId);
        xcUser.setWxUnionid(unionid);
        xcUser.setUsername(unionid);
        xcUser.setPassword(unionid);
        xcUser.setNickname(userInfo_map.get("nickname").toString());
        xcUser.setUserpic(userInfo_map.get("headimgurl").toString());
        xcUser.setName(userInfo_map.get("nickname").toString());
        xcUser.setUtype("101001");//学生类型
        xcUser.setStatus("1");//用户状态
        xcUser.setCreateTime(LocalDateTime.now());
        xcUserMapper.insert(xcUser);

        XcUserRole xcUserRole = new XcUserRole();
        xcUserRole.setId(UUID.randomUUID().toString());
        xcUserRole.setUserId(userId);
        xcUserRole.setRoleId("17");//学生角色
        xcUserRoleMapper.insert(xcUserRole);

        return xcUser;

    }
}
