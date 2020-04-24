package me.luliru.gateway.core.enums;

/**
 * CodeEnum
 * Created by luliru on 2019/7/8.
 */
public enum CodeEnum {

    SYS_MISSING_PARAMETER("sys.missing_parameter","缺少平台级参数"),
    SYS_INVALID_PARAMETER("sys.invalid_parameter","平台级参数不合法"),
    SYS_INVALID_SIGNATURE("sys.invalid_signature","无效签名"),
    SYS_API_NOT_EXISTED("sys.api_not_existed","api不存在"),
    SYS_EXPIRED_TIMESTAMP("sys.expired_timestamp","请求已过期"),
    SYS_UNKNOWN_ERROR("sys.unknown_error","平台未知错误"),
    API_TIMEOUT("api.timeout","api服务超时"),
    API_UNKNOWN_ERROR("api.unknown_error","api服务未知错误"),
    API_BUSINESS_ERROR("api.business_error","api业务异常"),
    API_MISSING_PARAMETER("api.missing_parameter","api缺少必要参数"),
    API_INVALID_PARAMETER("api.invalid_parameter","api参数不合法");

    private String code;

    private String message;

    CodeEnum(String code,String message){
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}