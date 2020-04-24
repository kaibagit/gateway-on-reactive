package me.luliru.gateway.core.process.dto;

import lombok.Data;

/**
 * RequestParam
 * Created by luliru on 2019/7/29.
 */
@Data
public class RequestParam {

    String appkey = null;
    String timestampStr = null;
    String nonce = null;
    String sign = null;
    String access_token = null;
    String api = null;
    String biz_param;
}
