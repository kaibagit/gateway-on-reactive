package me.luliru.gateway.core.process.processor;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import me.luliru.gateway.core.RequestContext;
import me.luliru.gateway.core.domain.resp.GatewayResponse;
import me.luliru.gateway.core.enums.CodeEnum;
import me.luliru.gateway.core.exception.ApiBizException;
import me.luliru.gateway.core.exception.OpenGatewayException;
import me.luliru.gateway.core.process.Processor;
import reactor.core.publisher.Mono;

/**
 * ExceptionHandleProcess
 * Created by luliru on 2019/7/29.
 */
@Slf4j
public class ExceptionHandleProcess implements Processor<String,String> {

    @Override
    public Mono<String> process(RequestContext ctx, Mono<String> mono) {
        return mono.onErrorResume(OpenGatewayException.class, e -> {
            GatewayResponse gatewayResponse = new GatewayResponse();
            gatewayResponse.setCode(e.getCode());
            gatewayResponse.setMessage(e.getMessage());
            return Mono.just(JSON.toJSONString(gatewayResponse));
        }).onErrorResume(ApiBizException.class, e->{
            GatewayResponse gatewayResponse = new GatewayResponse();
            gatewayResponse.setCode(CodeEnum.API_BUSINESS_ERROR.getCode());
            gatewayResponse.setMessage(CodeEnum.API_BUSINESS_ERROR.getMessage());
            gatewayResponse.setSub_code(e.getCode());
            gatewayResponse.setSub_message(e.getMessage());
            return Mono.just(JSON.toJSONString(gatewayResponse));
        }).onErrorResume(e ->{
            log.error("系统未知异常",e);
            GatewayResponse gatewayResponse = new GatewayResponse();
            gatewayResponse.setCode(CodeEnum.SYS_UNKNOWN_ERROR.getCode());
            gatewayResponse.setMessage(CodeEnum.SYS_UNKNOWN_ERROR.getMessage());
            return Mono.just(JSON.toJSONString(gatewayResponse));
        });
    }
}
