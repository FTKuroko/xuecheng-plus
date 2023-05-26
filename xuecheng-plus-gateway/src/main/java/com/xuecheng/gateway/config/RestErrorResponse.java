package com.xuecheng.gateway.config;

import java.io.Serializable;

/**
 * @author Kuroko
 * @description
 * @date 2023/5/25 19:28
 */
public class RestErrorResponse implements Serializable {

    private String errMessage;

    public RestErrorResponse(String errMessage){
        this.errMessage= errMessage;
    }

    public String getErrMessage() {
        return errMessage;
    }

    public void setErrMessage(String errMessage) {
        this.errMessage = errMessage;
    }
}
