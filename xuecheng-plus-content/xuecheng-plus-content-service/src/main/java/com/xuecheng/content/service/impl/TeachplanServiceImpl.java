package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.mapper.TeachplanMapper;
import com.xuecheng.content.mapper.TeachplanMediaMapper;
import com.xuecheng.content.model.dto.BindTeachplanMediaDto;
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

    @Override
    @Transactional
    public void orderByTeachplan(String moveType, Long teachplanId) {
        // 1. 获取当前课程计划
        Teachplan teachplan = teachplanMapper.selectById(teachplanId);
        // 2. 获取当前课程计划层级及序号
        Integer grade = teachplan.getGrade();
        Integer orderby = teachplan.getOrderby();
        // 课程 id
        Long courseId = teachplan.getCourseId();
        // 所属章节(父节点) id
        Long parentid = teachplan.getParentid();
        LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
        if("moveup".equals(moveType)){
            // 上移
            if(grade == 1){
                // 大章节上移
                // 找到同课程下上一个章节
                queryWrapper.eq(Teachplan::getCourseId, courseId)
                        .eq(Teachplan::getGrade, grade)
                        .lt(Teachplan::getOrderby, orderby)
                        .orderByDesc(Teachplan::getOrderby)
                        .last("LIMIT 1");
                Teachplan change = teachplanMapper.selectOne(queryWrapper);
                // 交换两个章节的顺序
                exchangeOrderby(teachplan, change);
            }else{
                // 小节上移
                // 找到同课程同章节下的上一个小节
                queryWrapper.eq(Teachplan::getCourseId, courseId)
                        .eq(Teachplan::getParentid, parentid)
                        .eq(Teachplan::getGrade, grade)
                        .lt(Teachplan::getOrderby, orderby)
                        .orderByDesc(Teachplan::getOrderby)
                        .last("LIMIT 1");
                Teachplan change = teachplanMapper.selectOne(queryWrapper);
                // 交换两个小节的顺序
                exchangeOrderby(teachplan, change);
            }
        }else if("movedown".equals(moveType)){
            // 下移
            if(grade == 1){
                // 大章节下移
                // 找到同课程下的下一个章节
                queryWrapper.eq(Teachplan::getCourseId, courseId)
                        .eq(Teachplan::getGrade, grade)
                        .gt(Teachplan::getOrderby, orderby)
                        .orderByAsc(Teachplan::getOrderby)
                        .last("LIMIT 1");
                Teachplan change = teachplanMapper.selectOne(queryWrapper);
                // 交换两个章节的顺序
                exchangeOrderby(teachplan, change);
            }else{
                // 小节下移
                // 找到同课程同章节下的下一个小节
                queryWrapper.eq(Teachplan::getCourseId, courseId)
                        .eq(Teachplan::getParentid, parentid)
                        .eq(Teachplan::getGrade, grade)
                        .gt(Teachplan::getOrderby, orderby)
                        .orderByAsc(Teachplan::getOrderby)
                        .last("LIMIT 1");
                Teachplan change = teachplanMapper.selectOne(queryWrapper);
                // 交换两个小节的顺序
                exchangeOrderby(teachplan, change);
            }
        }
    }

    /**
     * 交换两课程计划的顺序
     * @param teachplan
     * @param change
     */
    private void exchangeOrderby(Teachplan teachplan, Teachplan change){
        if(change == null){
            log.info("已经到头了，不能移动。");
            return;
        }
        Integer orderby = teachplan.getOrderby();
        Integer changeOrderby = change.getOrderby();
        teachplan.setOrderby(changeOrderby);
        change.setOrderby(orderby);
        teachplanMapper.updateById(teachplan);
        teachplanMapper.updateById(change);
    }

    @Override
    @Transactional
    public void associationMedia(BindTeachplanMediaDto bindTeachplanMediaDto) {
        // 教学计划 id
        Long teachplanId = bindTeachplanMediaDto.getTeachplanId();
        Teachplan teachplan = teachplanMapper.selectById(teachplanId);
        if(teachplan == null){
            XueChengPlusException.cast("教学计划不存在");
        }
        // 获取教学计划层级，第一级表示大目录，只有小节才能添加媒资信息，这里只有两级目录，因此不等于 2 时返回错误信息
        Integer grade = teachplan.getGrade();
        if(grade != 2){
            XueChengPlusException.cast("只允许第二级教学计划绑定媒资");
        }
        // 课程 id
        Long courseId = teachplan.getCourseId();
        // 绑定媒资，如果之前已经绑定过了媒资，再次绑定时为更新操作
        LambdaQueryWrapper<TeachplanMedia> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TeachplanMedia::getTeachplanId, teachplanId);
        // 先删除原来该教学计划绑定的媒资
        teachplanMediaMapper.delete(queryWrapper);
        // 再添加新的教学计划绑定媒资关系
        TeachplanMedia teachplanMedia = new TeachplanMedia();
        teachplanMedia.setTeachplanId(teachplanId);
        teachplanMedia.setMediaId(bindTeachplanMediaDto.getMediaId());
        teachplanMedia.setMediaFilename(bindTeachplanMediaDto.getFileName());
        teachplanMedia.setCourseId(courseId);
        teachplanMedia.setCreateDate(LocalDateTime.now());
        teachplanMediaMapper.insert(teachplanMedia);
    }

    @Override
    public void unassociationMedia(Long teachPlanId, String mediaId) {
        LambdaQueryWrapper<TeachplanMedia> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TeachplanMedia::getTeachplanId, teachPlanId)
                .eq(TeachplanMedia::getMediaId, mediaId);
        teachplanMediaMapper.delete(queryWrapper);
    }

}
