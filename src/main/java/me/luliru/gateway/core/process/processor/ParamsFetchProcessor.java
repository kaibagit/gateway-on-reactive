package me.luliru.gateway.core.process.processor;

import io.netty.handler.codec.http.FullHttpRequest;
import lombok.extern.slf4j.Slf4j;
import me.luliru.gateway.core.RequestContext;
import me.luliru.gateway.core.enums.CodeEnum;
import me.luliru.gateway.core.exception.OpenGatewayException;
import me.luliru.gateway.core.process.Processor;
import me.luliru.gateway.core.process.dto.RequestParam;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * ParamsFetchProcessor
 * Created by luliru on 2019/7/29.
 */
@Slf4j
public class ParamsFetchProcessor implements Processor<FullHttpRequest,RequestParam> {

    @Override
    public Mono<RequestParam> process(RequestContext ctx, Mono<FullHttpRequest> mono) {
        return mono.flatMap(request -> {
            log.info("【{}】 url:{}",ctx.id(),request.uri());
            String queryString = getQueryString(request);
            String biz_params = request.content().toString(Charset.defaultCharset());
            log.info("【{}】 biz_params:{}",ctx.id(),biz_params);

            // 平台级参数，null值表示未传参，""表示传空值
            String appkey = null;
            String timestampStr = null;
            Long timestamp = null;
            String nonce = null;
            String sign = null;
            String access_token = null;
            String api = null;
            Map<String,String> queryParameterMap = new HashMap<>();
            if(StringUtils.isNotBlank(queryString)){
                String[] queryParameters = queryString.split("&");
                for(String param : queryParameters){
                    String[] paramNameAndValue = param.split("=");
                    if(paramNameAndValue.length==1){
                        queryParameterMap.put(paramNameAndValue[0],"");
                    }else{
                        queryParameterMap.put(paramNameAndValue[0],paramNameAndValue[1]);
                    }
                }
                appkey = queryParameterMap.get("appkey");
                timestampStr = queryParameterMap.get("timestamp");
                nonce = queryParameterMap.get("nonce");
                sign = queryParameterMap.get("sign");
                access_token = queryParameterMap.get("access_token");
                api = queryParameterMap.get("api");
            }

            RequestParam requestParam = new RequestParam();
            requestParam.setAppkey(appkey);
            requestParam.setTimestampStr(timestampStr);
            requestParam.setNonce(nonce);
            requestParam.setSign(sign);
            requestParam.setAccess_token(access_token);
            requestParam.setApi(api);
            requestParam.setBiz_param(biz_params);
            ctx.put("requestParam",requestParam);

            // 校验必传参数
            if(appkey == null){
                return Mono.error(new OpenGatewayException(CodeEnum.SYS_MISSING_PARAMETER,"appkey不能为空"));
            }
            if(timestampStr == null){
                return Mono.error(new OpenGatewayException(CodeEnum.SYS_MISSING_PARAMETER,"timestamp不能为空"));
            }
            if(nonce == null){
                return Mono.error(new OpenGatewayException(CodeEnum.SYS_MISSING_PARAMETER,"nonce不能为空"));
            }
            if(api == null){
                return Mono.error(new OpenGatewayException(CodeEnum.SYS_MISSING_PARAMETER,"api不能为空"));
            }
            if(sign == null){
                return Mono.error(new OpenGatewayException(CodeEnum.SYS_MISSING_PARAMETER,"sign不能为空"));
            }

            // 校验数据格式
            if(StringUtils.isBlank(appkey)){
                return Mono.error(new OpenGatewayException(CodeEnum.SYS_INVALID_PARAMETER,"appkey不能为空"));
            }
            if(appkey.indexOf("t") != 0){
                return Mono.error(new OpenGatewayException(CodeEnum.SYS_INVALID_PARAMETER,"appkey不正确，请使用测试appkey"));
            }
            if(StringUtils.isBlank(timestampStr)){
                return Mono.error(new OpenGatewayException(CodeEnum.SYS_INVALID_PARAMETER,"timestamp不能为空"));
            }
            try{
                timestamp = Long.valueOf(timestampStr);
            }catch (NumberFormatException e){
                return Mono.error(new OpenGatewayException(CodeEnum.SYS_INVALID_PARAMETER,"timestamp格式不正确"));
            }
            if(StringUtils.isBlank(nonce)){
                return Mono.error(new OpenGatewayException(CodeEnum.SYS_INVALID_PARAMETER,"nonce不能为空"));
            }
            if(StringUtils.isBlank(api)){
                return Mono.error(new OpenGatewayException(CodeEnum.SYS_INVALID_PARAMETER,"api不能为空"));
            }
            if(StringUtils.isBlank(sign)){
                return Mono.error(new OpenGatewayException(CodeEnum.SYS_MISSING_PARAMETER,"sign不能为空"));
            }

            return Mono.just(requestParam);
        });
    }

    private String getQueryString(FullHttpRequest req){
        String[] uriAndQuery = req.uri().split("\\?");
        if(uriAndQuery.length > 1){
            return uriAndQuery[1];
        }
        return null;
    }
}
