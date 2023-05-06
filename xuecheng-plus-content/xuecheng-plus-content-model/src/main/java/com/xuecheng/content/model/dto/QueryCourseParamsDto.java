package com.xuecheng.content.model.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Kuroko
 * @description 课程查询参数 Dto
 * @date 2023/5/6 10:38
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class QueryCourseParamsDto {
    // 审核状态
    @ApiModelProperty("审核状态")
    private String auditStatus;
    // 课程名称
    @ApiModelProperty("课程名称")
    private String courseName;
    // 发布状态
    private String publishStatus;
}
