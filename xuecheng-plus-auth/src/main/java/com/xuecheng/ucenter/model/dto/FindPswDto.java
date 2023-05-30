package com.xuecheng.ucenter.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Kuroko
 * @description
 * @date 2023/5/30 14:43
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FindPswDto {
    String cellphone;
    String email;
    String checkcodekey;
    String checkcode;
    String password;
    String confirmpwd;
}
