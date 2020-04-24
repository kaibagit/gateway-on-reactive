package me.luliru.gateway.core.apiprocessor;

import com.alibaba.fastjson.JSON;
import com.dianwoba.order.domain.dto.result.OrderDTO;
import com.dianwoba.order.en.OrderStatusEn;
import com.dianwoda.open.order.mapping.dto.response.OrderMappingResponse;
import lombok.extern.slf4j.Slf4j;
import me.luliru.gateway.core.RequestContext;
import me.luliru.gateway.core.domain.req.TestApiOrderParam;
import me.luliru.gateway.core.enums.CodeEnum;
import me.luliru.gateway.core.exception.OpenGatewayException;
import me.luliru.gateway.core.manager.OrderManager;
import me.luliru.gateway.core.manager.OrderMappingManager;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import javax.annotation.Resource;

/**
 * AbstractTestOrderProcessor
 * Created by luliru on 2019/7/9.
 */
@Slf4j
public abstract class AbstractTestOrderProcessor<T> implements TestApiProcessor<T> {

    @Resource
    private OrderMappingManager orderMappingManager;

    @Resource
    private OrderManager orderManager;

    @Resource(name = "blockScheduler")
    private Scheduler blockScheduler;

    @Override
    public Mono<T> process(RequestContext ctx, TestApiContext context, String biz_params) {
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

       return Mono.defer(() ->{
           log.debug("AbstractTestOrderProcessor 处理...");
            try{
                OrderMappingResponse orderMappingResponse = orderMappingManager.fetchOrderMapping(order_original_id);
                int cityId = orderMappingResponse.getOrderMapping().getCityId();
                long orderId = orderMappingResponse.getOrderMapping().getDwdOrderId();
                OrderDTO order = orderManager.getOrderCauseException(cityId,orderId);
                return processOrder(cityId,order);
            }catch (Exception e){
                return Mono.error(e);
            }
        }).subscribeOn(blockScheduler).publishOn(ctx.scheduler());
    }

    public abstract Mono<T> processOrder(int cityId,OrderDTO order);

    protected String getOrderStatusDesc(int orderStatus){
        if(orderStatus == OrderStatusEn.PLACED.getCode()){
            return "已下单";
        }else if(orderStatus == OrderStatusEn.DISPATCHED.getCode()){
            return "已接单";
        }else if(orderStatus == OrderStatusEn.ARRIVED.getCode()){
            return "已到店";
        }else if(orderStatus == OrderStatusEn.OBTAINED.getCode()){
            return "已离店";
        }else if(orderStatus == OrderStatusEn.FINISHED.getCode()){
            return "已完成";
        }else if(orderStatus == OrderStatusEn.ABNORMAL.getCode()){
            return "异常";
        }else if(orderStatus == OrderStatusEn.CANCEL.getCode()){
            return "已取消";
        }else{
            return "未知";
        }
    }
}