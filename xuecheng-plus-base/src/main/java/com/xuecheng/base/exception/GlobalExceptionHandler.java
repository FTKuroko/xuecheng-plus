package com.xuecheng.base.exception;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Kuroko
 * @description 全局异常处理器
 * @date 2023/5/8 19:30
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {
    @ResponseBody
    @ExceptionHandler(XueChengPlusException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)   // 该异常错误码为500。返回自定义的异常
    public RestErrorResponse customException(XueChengPlusException e){
        log.error("系统异常:{}", e.getErrMessage());
        return new RestErrorResponse(e.getErrMessage());
    }

    @ResponseBody
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public RestErrorResponse exception(Exception e){
        log.error("系统异常:{}",e.getMessage(),e);
        e.printStackTrace();
        if(e.getMessage().equals("不允许访问")){
            return new RestErrorResponse("没有操作此功能的权限");
        }
        return new RestErrorResponse(CommonError.UNKOWN_ERROR.getErrMessage());

    }

    @ResponseBody
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public RestErrorResponse doMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        // 由于用户输入的内容可能存在多处错误，所以我们要将所有错误信息都提示给用户
        BindingResult bindingResult = exception.getBindingResult();
        // 获取错误集合
        List<FieldError> fieldErrors = bindingResult.getFieldErrors();
        // 拼接字符串
        StringBuffer stringBuffer = new StringBuffer();
        fieldErrors.forEach(fieldError -> stringBuffer.append(fieldError.getDefaultMessage()).append(","));
        // 记录日志
        log.error(stringBuffer.toString());
        // 响应给用户
        return new RestErrorResponse(stringBuffer.toString());
    }

}
