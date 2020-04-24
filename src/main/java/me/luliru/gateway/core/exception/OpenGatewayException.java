package me.luliru.gateway.core.exception;

import me.luliru.gateway.core.enums.CodeEnum;

/**
 * OpenGatewayException
 * Created by luliru on 2019/7/8.
 */
public class OpenGatewayException extends RuntimeException{

    private String code;

    public OpenGatewayException(CodeEnum codeEnum){
        this(codeEnum.getCode(),codeEnum.getMessage());
    }

    public OpenGatewayException(CodeEnum codeEnum, String message){
        this(codeEnum.getCode(),message);
    }

    public OpenGatewayException(String code,String message){
        super(message);
        this.code = code;
    }

//    public Throwable fillInStackTrace() {
//        return this;
//    }

    public String getCode() {
        return code;
    }
}
