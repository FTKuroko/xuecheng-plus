package com.xuecheng.base.exception;

/**
 * @author Kuroko
 * @description 学成在线项目异常类
 * @date 2023/5/8 19:18
 */
public class XueChengPlusException extends RuntimeException{
    private String errMessage;
    public XueChengPlusException(){
        super();
    }
    public XueChengPlusException(String errMessage){
        super(errMessage);
        this.errMessage = errMessage;
    }
    public String getErrMessage(){
        return errMessage;
    }
    public static void cast(CommonError commonError){
        throw new XueChengPlusException(commonError.getErrMessage());
    }
    public static void cast(String errMessage){
        throw new XueChengPlusException(errMessage);
    }
}
