package com.xuecheng.ucenter.service;

import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;

/**
 * @author Kuroko
 * @description 认证 service
 * @date 2023/5/26 17:04
 */
public interface AuthService {
    /**
     * 认证方法
     * @param authParamsDto 认证参数
     * @return
     */
    XcUserExt excute(AuthParamsDto authParamsDto);
}
