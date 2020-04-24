package me.luliru.gateway.core.domain.resp;

import lombok.Data;

/**
 * GatewayResponse
 * Created by luliru on 2019/7/8.
 */
@Data
public class GatewayResponse {

    private String code = "success";

    private String message = "成功";

    private String sub_code;

    private String sub_message;

    private Object data;
}
