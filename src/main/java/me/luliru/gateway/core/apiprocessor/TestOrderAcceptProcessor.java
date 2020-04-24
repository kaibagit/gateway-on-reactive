package me.luliru.gateway.core.apiprocessor;

import com.alibaba.dubbo.rpc.cluster.support.UnitUtil;
import com.dianwoba.dispatch.weather.monitor.domain.dto.param.TestDispatchParam;
import com.dianwoba.dispatch.weather.monitor.provider.TestDispatchProvider;
import com.dianwoba.order.domain.dto.result.OrderDTO;
import com.dianwoba.order.en.OrderStatusEn;
import lombok.extern.slf4j.Slf4j;
import me.luliru.gateway.core.enums.SubCodeEnum;
import me.luliru.gateway.core.exception.ApiBizException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;

/**
 * TestOrderAcceptProcessor
 * Created by luliru on 2019/7/9.
 */
@Slf4j
@Service
public class TestOrderAcceptProcessor extends AbstractTestOrderProcessor {

    @Resource
    private TestDispatchProvider testDispatchProvider;

    @Override
    public Mono processOrder(int cityId, OrderDTO order) {
        log.debug("TestOrderAcceptProcessor 处理...");
        // 查出当前订单状态，并判断能否状态转化
        if (!(order.getStatus() == OrderStatusEn.PLACED.getCode())) {
            return Mono.error(new ApiBizException(SubCodeEnum.ORDER_NOT_SUPPORT_MODIFICATION,"不能从当前订单状态'"+getOrderStatusDesc(order.getStatus())+"'转换为'已接单'"));
        }

        try{
            TestDispatchParam dispatchParam = new TestDispatchParam();
            dispatchParam.setCityId(order.getCityId());
            dispatchParam.setOrderId(order.getId());
            dispatchParam.setRiderId(1);
            dispatchParam.setRiderLat(order.getFromLat());
            dispatchParam.setRiderLng(order.getFromLng());

            UnitUtil.setCityId(cityId);
            boolean isDispatchSuccess = testDispatchProvider.dispatch(dispatchParam);
            if (!isDispatchSuccess) {
                return Mono.error(new ApiBizException(SubCodeEnum.ORDER_NOT_SUPPORT_MODIFICATION,"调度系统模拟派单失败！"));
            }
            return Mono.empty();
        }catch (Exception e){
            return Mono.error(e);
        }
    }
}
