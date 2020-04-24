package me.luliru.gateway.core.enums;

/**
 * SubCodeEnum
 * Created by luliru on 2019/7/8.
 */
public enum SubCodeEnum {

    ORDER_NOT_EXISTED("order_not_existed","订单不存在"),
    ORDER_NOT_SUPPORT_MODIFICATION("order_not_support_modification","当前状态不允许修改");

    private String code;

    private String message;

    SubCodeEnum(String code,String message){
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