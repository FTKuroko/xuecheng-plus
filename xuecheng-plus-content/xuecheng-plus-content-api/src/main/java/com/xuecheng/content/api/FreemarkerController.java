package com.xuecheng.content.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * @author Kuroko
 * @description
 * @date 2023/5/16 16:37
 */
@Controller
public class FreemarkerController {
    @GetMapping("/testfreemarker")
    public ModelAndView test(){
        ModelAndView modelAndView = new ModelAndView();
        // 设置模型数据
        modelAndView.addObject("name", "kuroko");
        // 设置模板名称
        modelAndView.setViewName("test");
        return modelAndView;
    }
}
