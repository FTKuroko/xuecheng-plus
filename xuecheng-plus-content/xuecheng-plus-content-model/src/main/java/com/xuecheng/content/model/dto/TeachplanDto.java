package com.xuecheng.content.model.dto;

import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.model.po.TeachplanMedia;
import lombok.Data;

import java.util.List;

/**
 * @author Kuroko
 * @description 课程计划树形结构 Dto
 * @date 2023/5/9 15:50
 */
@Data
public class TeachplanDto extends Teachplan {
    // 课程计划关联的媒资信息
    private TeachplanMedia teachplanMedia;
    // 子节点
    private List<TeachplanDto> teachPlanTreeNodes;
}
