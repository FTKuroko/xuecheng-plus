package com.xuecheng.base.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * @author Kuroko
 * @description 分页查询结果模型类
 * @date 2023/5/6 10:40
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageResult<T> implements Serializable {
    // 数据列表
    private List<T> items;
    // 总记录数
    private long counts;
    // 当前页码
    private long page;
    // 每页记录数
    private long pageSize;

}
