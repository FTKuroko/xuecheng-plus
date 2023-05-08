package com.xuecheng.content;

import com.xuecheng.content.model.dto.CourseCategoryTreeDto;
import com.xuecheng.content.service.CourseCatogoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 * @author Kuroko
 * @description
 * @date 2023/5/8 15:03
 */
@SpringBootTest
public class CourseCategoryServiceTest {
    @Autowired
    CourseCatogoryService courseCatogoryService;

    @Test
    void testqueryTreeNodes() {
        List<CourseCategoryTreeDto> categoryTreeDtos = courseCatogoryService.queryTreeNodes("1");
        System.out.println(categoryTreeDtos);
    }

}
