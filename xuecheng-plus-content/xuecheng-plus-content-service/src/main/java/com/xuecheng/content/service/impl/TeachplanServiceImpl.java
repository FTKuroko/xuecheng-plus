package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.mapper.TeachplanMapper;
import com.xuecheng.content.mapper.TeachplanMediaMapper;
import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.model.po.TeachplanMedia;
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

    @Autowired
    private TeachplanMediaMapper teachplanMediaMapper;

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

    /**
     * 根据课程计划 id 删除课程计划
     * @param teachplanId
     */
    @Override
    @Transactional
    public void deleteTeachplan(Long teachplanId) {
        // 1. 判断当前课程计划是否为空
        if(teachplanId == null) XueChengPlusException.cast("课程计划 id 为空!");
        LambdaQueryWrapper<Teachplan> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        // 2. 查询当前课程计划 id 下是否有子节
        lambdaQueryWrapper.eq(Teachplan::getParentid, teachplanId);
        Integer count = teachplanMapper.selectCount(lambdaQueryWrapper);
        if(count > 0){
            // 2.1 删除大章节，大章节下有小章节时不允许删除。
            XueChengPlusException.cast("课程计划信息还有自己信息，不能删除!");
        }else{
            // 2.2 删除大章节，大单节下没有小章节时可以正常删除。
            teachplanMapper.deleteById(teachplanId);
            // 2.3 删除小章节，同时将关联的信息进行删除。
            LambdaQueryWrapper<TeachplanMedia> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(TeachplanMedia::getTeachplanId, teachplanId);
            teachplanMediaMapper.delete(queryWrapper);
        }



    }
}
