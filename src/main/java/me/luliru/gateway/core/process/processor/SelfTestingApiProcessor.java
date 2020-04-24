package me.luliru.gateway.core.process.processor;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import me.luliru.gateway.core.RequestContext;
import me.luliru.gateway.core.domain.resp.GatewayResponse;
import me.luliru.gateway.core.enums.CodeEnum;
import me.luliru.gateway.core.exception.OpenGatewayException;
import me.luliru.gateway.core.process.Processor;
import me.luliru.gateway.core.apiprocessor.TestApiContext;
import me.luliru.gateway.core.apiprocessor.TestApiProcessor;
import me.luliru.gateway.core.process.dto.RequestParam;
import me.luliru.gateway.core.repository.entity.ApplicationQa;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * SelfTestingApiProcessor
 * Created by luliru on 2019/7/30.
 */
@Slf4j
public class SelfTestingApiProcessor implements Processor<ApplicationQa,String> {

    private Map<String,TestApiProcessor> testApiProcessorMap = new HashMap<>();

    @Override
    public Mono<String> process(RequestContext ctx, Mono<ApplicationQa> mono) {
        return mono.flatMap(applicationQa -> {
            RequestParam requestParam = (RequestParam) ctx.get("requestParam");
            TestApiContext context = new TestApiContext();
            context.setApi(requestParam.getApi());
            context.setAppkey(requestParam.getAppkey());
            return processTestApi(ctx,context,requestParam.getBiz_param()).map(t -> JSON.toJSONString(new GatewayResponse()));
        });
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
