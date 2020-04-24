package me.luliru.gateway.core.mock.v1;

import com.alibaba.fastjson.JSON;
import com.dianwoba.dispatch.weather.monitor.provider.TestDispatchProvider;
import lombok.extern.slf4j.Slf4j;
import me.luliru.gateway.core.RequestContext;
import me.luliru.gateway.core.apiprocessor.TestApiContext;
import me.luliru.gateway.core.apiprocessor.TestApiProcessor;
import me.luliru.gateway.core.domain.req.TestApiOrderParam;
import me.luliru.gateway.core.enums.CodeEnum;
import me.luliru.gateway.core.exception.OpenGatewayException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import javax.annotation.Resource;
import java.util.Map;

/**
 * MockTestApiProcessor
 * Created by luliru on 2019/8/1.
 */
@Slf4j
@Service
public class MockTestApiProcessor implements TestApiProcessor {

    @Resource
    private TestDispatchProvider testDispatchProvider;

    @Resource(name = "blockScheduler")
    private Scheduler blockScheduler;

    @Override
    public Mono process(RequestContext ctx, TestApiContext context, String biz_params) {
        if(StringUtils.isBlank(biz_params)){
            return Mono.error(new OpenGatewayException(CodeEnum.API_MISSING_PARAMETER,"order_original_id不能为空"));
        }
        TestApiOrderParam param = null;
        try{
            param = JSON.parseObject(biz_params, TestApiOrderParam.class);
        }catch (Exception e){
            log.error("解析业务参数失败",e);
            return Mono.error(new OpenGatewayException(CodeEnum.API_INVALID_PARAMETER,"解析业务参数失败"));
        }
        String order_original_id = param.getOrder_original_id();
        if(StringUtils.isBlank(order_original_id)){
            return Mono.error(new OpenGatewayException(CodeEnum.API_MISSING_PARAMETER,"order_original_id不能为空"));
        }

        costCpu(biz_params);

        return Mono.defer(() ->{
            log.debug("MockTestApiProcessor 处理...");
            try {
                Thread.sleep(300L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            costCpu(biz_params);
            return Mono.just(1);
        }).subscribeOn(blockScheduler).publishOn(ctx.scheduler());
    }

    /**
     * 模拟序列化等CPU消耗
     * @param biz_params
     */
    private void costCpu(String biz_params){
        for(int i=0;i<3;i++){
            JSON.parseObject(biz_params,Map.class);
        }
    }
}
