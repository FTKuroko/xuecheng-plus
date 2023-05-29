package com.xuecheng.content;

import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.service.CourseBaseInfoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author Kuroko
 * @description
 * @date 2023/5/6 17:13
 */
@SpringBootTest
public class CourseBaseInfoServiceTest {
    @Autowired
    CourseBaseInfoService courseBaseInfoService;

    @Test
    void test(){
        // 查询条件
        QueryCourseParamsDto queryCourseParamsDto = new QueryCourseParamsDto();
        queryCourseParamsDto.setCourseName("java");
        queryCourseParamsDto.setAuditStatus("202004");
        queryCourseParamsDto.setPublishStatus("203001");

        // 分页参数
        PageParams pageParams = new PageParams(1L, 3L);

        // 结果集
        PageResult<CourseBase> courseBasePageResult = courseBaseInfoService.queryCourseBaseList(null, pageParams, queryCourseParamsDto);
        System.out.println(courseBasePageResult);
    }
}
