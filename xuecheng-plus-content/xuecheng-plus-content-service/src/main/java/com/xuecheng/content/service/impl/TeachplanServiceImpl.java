package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.mapper.TeachplanMapper;
import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.service.TeachplanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author Kuroko
 * @description
 * @date 2023/5/9 16:03
 */
@Slf4j
@Service
public class TeachplanServiceImpl implements TeachplanService {
    @Autowired
    private TeachplanMapper teachplanMapper;

    /**
     * 根据课程id查询课程计划
     * @param courseId
     * @return
     */
    @Override
    public List<TeachplanDto> findTeachplanTree(Long courseId) {
        return teachplanMapper.selectTreeNodes(courseId);
    }

    /**
     * 创建或修改课程计划
     * @param teachplanDto
     */
    @Override
    @Transactional
    public void saveTeachplan(SaveTeachplanDto teachplanDto) {
        // 1.获取课程i计划id判断是创建还是修改操作
        Long id = teachplanDto.getId();

        if(id != null){
            // 修改课程计划
            Teachplan teachplan = teachplanMapper.selectById(id);
            BeanUtils.copyProperties(teachplanDto, teachplan);
            teachplan.setChangeDate(LocalDateTime.now());
            int flag = teachplanMapper.updateById(teachplan);
            if(flag <= 0) XueChengPlusException.cast("修改课程计划失败!");
        }else{
            // 创建课程计划
            Teachplan teachplan = new Teachplan();
            BeanUtils.copyProperties(teachplanDto, teachplan);
            teachplan.setCreateDate(LocalDateTime.now());
            // 取出同父同级别的课程计划数量，设置排序号（原课程计划下已有子节点数加一）
            teachplan.setOrderby(getTeachplanCount(teachplanDto.getCourseId(), teachplanDto.getParentid()) + 1);
            int insert = teachplanMapper.insert(teachplan);
            if(insert <= 0) XueChengPlusException.cast("新增课程计划失败!");
        }
    }

    /**
     * 获取最新的排序号。统计数据库中该课程下父课程计划的子节点个数
     * @param courseId 课程 id
     * @param parentId 父课程计划 id
     * @return
     */
    private int getTeachplanCount(Long courseId, Long parentId){
        LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Teachplan::getCourseId, courseId);
        queryWrapper.eq(Teachplan::getParentid, parentId);
        return teachplanMapper.selectCount(queryWrapper);
    }
}
