package me.luliru.gateway.core.process.processor;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import me.luliru.gateway.core.RequestContext;
import me.luliru.gateway.core.domain.resp.GatewayResponse;
import me.luliru.gateway.core.enums.CodeEnum;
import me.luliru.gateway.core.exception.OpenGatewayException;
import me.luliru.gateway.core.process.Processor;
import me.luliru.gateway.core.process.dto.RequestParam;
import me.luliru.gateway.core.netty.HttpClient;
import me.luliru.gateway.core.apiprocessor.TestApiContext;
import me.luliru.gateway.core.apiprocessor.TestApiProcessor;
import me.luliru.gateway.core.repository.entity.ApplicationQa;
import me.luliru.gateway.core.util.Signer;
import org.apache.commons.lang3.tuple.Pair;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * ApiInvokeProcessor
 * Created by luliru on 2019/7/29.
 */
@Slf4j
public class ApiInvokeProcessor implements Processor<ApplicationQa,String> {

    private String qaAccessToken = "TEST2018-a444-4e50-b785-f48ba984bd9c";

    private String remoteAppkey = "1000024";

    private String remoteSecret = "1b29ef6f5826878c7f3243d0a0495a99";

    private String remoteAccessToken = "11d28381-41e7-4983-8392-f7a40cb87245";

    private String remoteGateway = "http://open-api-gw-gw-dev-gw.dwbops.com/gateway";

    private Map<String,TestApiProcessor> testApiProcessorMap = new HashMap<>();

    private HttpClient httpClient = new HttpClient();

    @Override
    public Mono<String> process(RequestContext ctx, Mono<ApplicationQa> mono) {
        return mono.flatMap(applicationQa -> {
            RequestParam requestParam = (RequestParam) ctx.get("requestParam");
            if(isSelfTestingApi(requestParam.getApi())){
                TestApiContext context = new TestApiContext();
                context.setApi(requestParam.getApi());
                context.setAppkey(requestParam.getAppkey());
                return processTestApi(ctx,context,requestParam.getBiz_param()).map(t -> JSON.toJSONString(new GatewayResponse()));
            }else{
                // 准备调用QA环境开放平台网关
                // 需要将appkey和secret切换成qa环境准备好的appkey和secret
                Pair<String,String> unsignStringAndFinalSign = Signer.getUnsignStringAndFinalSign(remoteAppkey, requestParam.getTimestampStr(), requestParam.getNonce(),remoteAccessToken, requestParam.getApi(),remoteSecret,requestParam.getBiz_param());
                log.info("【{}】 remote gateway unsigned string : {}",ctx.id(),unsignStringAndFinalSign.getLeft());
                String remoteSign = unsignStringAndFinalSign.getRight();
                // 构建url
                Map<String,String> remoteGatewayQueryParams = new HashMap<>();
                remoteGatewayQueryParams.put("timestamp",requestParam.getTimestampStr());
                remoteGatewayQueryParams.put("nonce",requestParam.getNonce());
                remoteGatewayQueryParams.put("api",requestParam.getApi());
                // 需要替换appkey和sign
                remoteGatewayQueryParams.put("appkey", remoteAppkey);
                remoteGatewayQueryParams.put("access_token",remoteAccessToken);
                remoteGatewayQueryParams.put("sign",remoteSign);

                // 拼接url
                StringBuilder requestUrl = new StringBuilder(remoteGateway);
                if(!remoteGateway.contains("?")){
                    requestUrl.append("?");
                }
                boolean firstQaGatewayQueryParam = true;
                for(Map.Entry<String,String> entry : remoteGatewayQueryParams.entrySet()){
                    if(!firstQaGatewayQueryParam){
                        requestUrl.append("&");
                    }
                    requestUrl.append(entry.getKey()).append("=").append(entry.getValue());
                    firstQaGatewayQueryParam = false;
                }

                return httpClient.post(ctx.eventLoop(),requestUrl.toString(),requestParam.getBiz_param());
            }
        });
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
}
