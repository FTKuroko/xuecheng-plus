package com.xuecheng.base.model;

/**
 * @author Kuroko
 * @date 2023/5/6 10:29
 */

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分页查询通用参数
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageParams {
    // 默认起始页码
    private static final long DEFAULT_PAGE_CURRENT = 1L;
    // 默认每页记录数
    private static final long DEFAULT_PAGE_SIZE = 10L;

    // 当前页码
    @ApiModelProperty("当前页码")
    private Long pageNo = DEFAULT_PAGE_CURRENT;
    // 每页记录数默认值
    @ApiModelProperty("当前每页记录数")
    private Long pageSize = DEFAULT_PAGE_SIZE;

}
