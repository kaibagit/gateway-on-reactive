package me.luliru.gateway.core.manager;

import com.alibaba.dubbo.rpc.cluster.support.UnitUtil;
import com.dianwoba.order.domain.dto.param.BaseParam;
import com.dianwoba.order.domain.dto.result.OrderDTO;
import com.dianwoba.order.query.provider.QueryOrderProvider;
import me.luliru.gateway.core.enums.SubCodeEnum;
import me.luliru.gateway.core.exception.ApiBizException;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * OrderManager
 * Created by luliru on 2019/7/9.
 */
@Component
public class OrderManager {

    @Resource
    private QueryOrderProvider queryOrderProvider;

    public OrderDTO getOrderCauseException(Integer cityId, Long orderId) {
        OrderDTO order = findOrderByIdDeeply(cityId, orderId);
        if (order == null) {
            throw new ApiBizException(SubCodeEnum.ORDER_NOT_EXISTED);
        }
        return order;
    }

    public OrderDTO findOrderByIdDeeply(int cityId, Long orderId) {
        UnitUtil.setCityId(cityId);
        BaseParam queryParam = new BaseParam();
        queryParam.setCityId(cityId);
        queryParam.setOrderId(orderId);
        return queryOrderProvider.findOrderByIdDeeply(queryParam);
    }
}
