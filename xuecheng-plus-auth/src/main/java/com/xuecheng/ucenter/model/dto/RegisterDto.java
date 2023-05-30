package com.xuecheng.ucenter.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Kuroko
 * @description 注册用户输入数据
 * @date 2023/5/30 16:17
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterDto {
    private String cellphone;

    private String checkcode;

    private String checkcodekey;

    private String confirmpwd;

    private String email;

    private String nickname;

    private String password;

    private String username;
}
