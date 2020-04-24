package me.luliru.gateway.core.mock.v1;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpUtil;
import lombok.extern.slf4j.Slf4j;
import me.luliru.gateway.core.RequestContext;
import me.luliru.gateway.core.apiprocessor.TestApiContext;
import me.luliru.gateway.core.apiprocessor.TestApiProcessor;
import me.luliru.gateway.core.domain.resp.GatewayResponse;
import me.luliru.gateway.core.enums.CodeEnum;
import me.luliru.gateway.core.exception.ApiBizException;
import me.luliru.gateway.core.exception.OpenGatewayException;
import me.luliru.gateway.core.netty.HttpClient;
import me.luliru.gateway.core.repository.entity.ApplicationQa;
import me.luliru.gateway.core.util.Signer;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import static io.netty.handler.codec.http.HttpHeaderValues.KEEP_ALIVE;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * MockGatewayInboundHandler
 * Created by luliru on 2019/8/1.
 */
@Slf4j
@ChannelHandler.Sharable
public class MockGatewayInboundHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private String qaAccessToken = "TEST2018-a444-4e50-b785-f48ba984bd9c";

    private String remoteAppkey = "1000024";

    private String remoteSecret = "1b29ef6f5826878c7f3243d0a0495a99";

    private String remoteAccessToken = "11d28381-41e7-4983-8392-f7a40cb87245";

    private String remoteGateway = "http://open-api-gw-gw-dev-gw.dwbops.com/gateway";

    private Scheduler blockScheduler;

    private Map<String,TestApiProcessor> testApiProcessorMap = new HashMap<>();

    private HttpClient httpClient = new HttpClient();

    public MockGatewayInboundHandler(Scheduler blockScheduler){
        this.blockScheduler = blockScheduler;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request){
        RequestContext rctx = RequestContext.createContext(ctx);
        rctx.setKeepAlive(HttpUtil.isKeepAlive(request));
        log.info("【{}】 url:{}",rctx.id(),request.uri());
        String queryString = getQueryString(request);
        String biz_params = request.content().toString(Charset.defaultCharset());
        log.info("【{}】 biz_params:{}",rctx.id(),biz_params);
        invokeQaOpenApi(rctx,queryString,biz_params)
                .subscribe(json -> {
                    log.debug("【{}】 response writing.",rctx.id());
                    byte[] data = json.getBytes();
                    FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(data));
                    response.headers().set("Content-Type", "application/json;charset=UTF-8");
                    response.headers().set("Content-Length", Integer.toString(data.length));
                    if(!rctx.isKeepAlive()){
                        rctx.getChannelHandlerCtx().writeAndFlush(response).addListener(new ChannelFutureListener() {
                            @Override
                            public void operationComplete(ChannelFuture future) throws Exception {
                                RequestContext.destroyContext(rctx.getChannelHandlerCtx(),rctx);
                                future.channel().close();
                            }
                        });
                    }else{
                        response.headers().set(CONNECTION, KEEP_ALIVE);
                        rctx.getChannelHandlerCtx().writeAndFlush(response).addListener(new ChannelFutureListener(){

                            @Override
                            public void operationComplete(ChannelFuture future) throws Exception {
                                RequestContext.destroyContext(rctx.getChannelHandlerCtx(),rctx);
                            }
                        });
                    }
                });
    }

    public Mono<String> invokeQaOpenApi(RequestContext ctx, String queryString, String biz_params){
        return invokeQaOpenApi0(ctx,queryString,biz_params)
                .onErrorResume(OpenGatewayException.class, e -> {
                    GatewayResponse gatewayResponse = new GatewayResponse();
                    gatewayResponse.setCode(e.getCode());
                    gatewayResponse.setMessage(e.getMessage());
                    return Mono.just(JSON.toJSONString(gatewayResponse));
                })
                .onErrorResume(ApiBizException.class, e->{
                    GatewayResponse gatewayResponse = new GatewayResponse();
                    gatewayResponse.setCode(CodeEnum.API_BUSINESS_ERROR.getCode());
                    gatewayResponse.setMessage(CodeEnum.API_BUSINESS_ERROR.getMessage());
                    gatewayResponse.setSub_code(e.getCode());
                    gatewayResponse.setSub_message(e.getMessage());
                    return Mono.just(JSON.toJSONString(gatewayResponse));
                })
                .onErrorResume(e ->{
                    log.error("系统未知异常",e);
                    GatewayResponse gatewayResponse = new GatewayResponse();
                    gatewayResponse.setCode(CodeEnum.SYS_UNKNOWN_ERROR.getCode());
                    gatewayResponse.setMessage(CodeEnum.SYS_UNKNOWN_ERROR.getMessage());
                    return Mono.just(JSON.toJSONString(gatewayResponse));
                });
    }

    public Mono<String> invokeQaOpenApi0(RequestContext ctx,String queryString,String biz_params){
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

        String finalAppkey = appkey;
        String finalTimestampStr = timestampStr;
        String finalNonce = nonce;
        String finalAccess_token = access_token;
        String finalApi = api;
        String finalSign = sign;

        Mono<ApplicationQa> applicationQaMono = Mono.create(sink ->{
            log.debug("db operating.");
            ApplicationQa applicationQa = new ApplicationQa();
            applicationQa.setId(1L);
            applicationQa.setDevepId(1L);
            applicationQa.setAppName("app_name");
            applicationQa.setAppKey("app_key");
            applicationQa.setAppSecret("app_secret");
            applicationQa.setCallbackUrl("callback_url");
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            sink.success(applicationQa);
        });

        return applicationQaMono
                .subscribeOn(blockScheduler)
                .publishOn(ctx.scheduler())
                .flatMap(applicationQa -> {
                    log.info("【{}】 处理applicationQa",ctx.id());
                    // 验签
                    String generatedSign = Signer.sign(finalAppkey, finalTimestampStr, finalNonce, finalAccess_token, finalApi,applicationQa.getAppSecret(),biz_params);

                    // 验证access_token
                    boolean needAccessToken = needAccessToken(finalApi);
                    if(needAccessToken){
                        if(finalAccess_token == null){
                            return Mono.error(new OpenGatewayException(CodeEnum.SYS_MISSING_PARAMETER,"access_token不能为空"));
                        }
                        if(StringUtils.isBlank(finalAccess_token)){
                            return Mono.error(new OpenGatewayException(CodeEnum.SYS_INVALID_PARAMETER,"access_token不能为空"));
                        }
                        if(!qaAccessToken.equals(finalAccess_token)){
                            return Mono.error(new OpenGatewayException(CodeEnum.SYS_INVALID_PARAMETER,"无效的access_token"));
                        }
                    }

                    // qa环境特殊业务参数校验
                    validateBizParams(finalAppkey, finalApi,biz_params);

                    TestApiContext context = new TestApiContext();
                    context.setApi(finalApi);
                    context.setAppkey(finalAppkey);
                    return processTestApi(ctx,context,biz_params).map(t -> JSON.toJSONString(new GatewayResponse()));

//                    if(isSelfTestingApi(finalApi)){
//                        TestApiContext context = new TestApiContext();
//                        context.setApi(finalApi);
//                        context.setAppkey(finalAppkey);
//                        return processTestApi(ctx,context,biz_params).map(t -> JSON.toJSONString(new GatewayResponse()));
//                    }else{
//                        // 准备调用QA环境开放平台网关
//                        // 需要将appkey和secret切换成qa环境准备好的appkey和secret
//                        Pair<String,String> unsignStringAndFinalSign = Signer.getUnsignStringAndFinalSign(remoteAppkey, finalTimestampStr, finalNonce,remoteAccessToken, finalApi,remoteSecret,biz_params);
//                        log.info("【{}】 remote gateway unsigned string : {}",ctx.id(),unsignStringAndFinalSign.getLeft());
//                        String remoteSign = unsignStringAndFinalSign.getRight();
//                        // 构建url
//                        Map<String,String> remoteGatewayQueryParams = mapClone(queryParameterMap);
//                        // 需要替换appkey和sign
//                        remoteGatewayQueryParams.put("appkey", remoteAppkey);
//                        remoteGatewayQueryParams.put("access_token",remoteAccessToken);
//                        remoteGatewayQueryParams.put("sign",remoteSign);
//
//                        // 拼接url
//                        StringBuilder requestUrl = new StringBuilder(remoteGateway);
//                        if(!remoteGateway.contains("?")){
//                            requestUrl.append("?");
//                        }
//                        boolean firstQaGatewayQueryParam = true;
//                        for(Map.Entry<String,String> entry : remoteGatewayQueryParams.entrySet()){
//                            if(!firstQaGatewayQueryParam){
//                                requestUrl.append("&");
//                            }
//                            requestUrl.append(entry.getKey()).append("=").append(entry.getValue());
//                            firstQaGatewayQueryParam = false;
//                        }
//
//                        return httpClient.post(ctx.eventLoop(),requestUrl.toString(),biz_params);
//                    }
                });
    }

    private boolean needAccessToken(String api){
        return "dianwoda.data.city.code".equals(api)? false:true;
    }

    private void validateBizParams(String appkey,String api,String biz_params){
        try{
            switch (api){
                case "dianwoda.seller.transportation.confirm":
                    if(StringUtils.isNotBlank(biz_params)){
                        JSONObject data = JSON.parseObject(biz_params);
                        String seller_id = data.getString("seller_id");
                        if(StringUtils.isBlank(seller_id)){
                            throw new OpenGatewayException(CodeEnum.API_MISSING_PARAMETER,"seller_id不能为空");
                        }
                        if(seller_id.indexOf(appkey) != 0){
                            throw new OpenGatewayException(CodeEnum.API_INVALID_PARAMETER,"qa联调时，seller_id请加上'${appkey}_'前缀");
                        }
                    }
                    break;
                case "dianwoda.order.create":
                    if(StringUtils.isNotBlank(biz_params)){
                        JSONObject data = JSON.parseObject(biz_params);
                        String order_original_id = data.getString("order_original_id");
                        if(StringUtils.isBlank(order_original_id)){
                            throw new OpenGatewayException(CodeEnum.API_MISSING_PARAMETER,"order_original_id不能为空");
                        }
                        if(order_original_id.indexOf(appkey) != 0){
                            throw new OpenGatewayException(CodeEnum.API_INVALID_PARAMETER,"qa联调时，order_original_id请加上'${appkey}_'前缀");
                        }
                        String seller_id = data.getString("seller_id");
                        if(StringUtils.isBlank(seller_id)){
                            throw new OpenGatewayException(CodeEnum.API_MISSING_PARAMETER,"seller_id不能为空");
                        }
                        if(seller_id.indexOf(appkey) != 0){
                            throw new OpenGatewayException(CodeEnum.API_INVALID_PARAMETER,"qa联调时，seller_id请加上'${appkey}_'前缀");
                        }
                    }
                    break;
            }
        }catch (Exception e){
            if(e instanceof OpenGatewayException) throw e;
            log.error("validateBizParams failure",e);
            throw new OpenGatewayException(CodeEnum.API_BUSINESS_ERROR,"业务参数解析失败");
        }
    }

    private String getQueryString(FullHttpRequest req){
        String[] uriAndQuery = req.uri().split("\\?");
        if(uriAndQuery.length > 1){
            return uriAndQuery[1];
        }
        return null;
    }

    private boolean isSelfTestingApi(String api){
        if(api.indexOf("test.") == 0){
            return true;
        }
        return false;
    }

    private Mono processTestApi(RequestContext ctx,TestApiContext context,String biz_params){
        Mono single = null;
        String api = context.getApi();
        try{
            TestApiProcessor processor = testApiProcessorMap.get(api);
            if (processor == null){
                single = Mono.error(new OpenGatewayException(CodeEnum.SYS_API_NOT_EXISTED));
            }else{
                single = processor.process(ctx,context,biz_params);
            }
        }catch (Exception e){
            log.error("自测api处理异常",e);
            single = Mono.error(new OpenGatewayException(CodeEnum.API_BUSINESS_ERROR,"api业务系统异常，请稍后再试"));
        }
        return single;
    }

    public void registeTestApiProcessor(String api,TestApiProcessor processor){
        testApiProcessorMap.put(api,processor);
    }

    private Map<String,String> mapClone(Map<String,String> origin){
        Map<String,String> clone = new HashMap<>(origin.size());
        clone.putAll(origin);
        return clone;
    }

    public void channelInactive(ChannelHandlerContext ctx) {
        RequestContext.destroyContext(ctx);
        ctx.fireChannelInactive();
    }
}
