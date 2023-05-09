package com.xuecheng.content.model.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author Kuroko
 * @description 修改课程基本信息，与添加课程相比只是提交参数时多了一个 id 字段
 * @date 2023/5/9 11:11
 */
@Data
@ApiModel(value = "EditCourseDto", description = "修改课程基本信息")
public class EditCourseDto extends AddCourseDto{
    @ApiModelProperty(value = "课程id", required = true)
    private Long id;
}
