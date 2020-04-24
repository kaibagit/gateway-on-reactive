package me.luliru.gateway.core.manager;

import com.dianwoda.open.order.mapping.dto.request.QueryRequest;
import com.dianwoda.open.order.mapping.dto.response.OrderMappingResponse;
import com.dianwoda.open.order.mapping.provider.OrderMappingProvider;
import me.luliru.gateway.core.enums.CodeEnum;
import me.luliru.gateway.core.enums.SubCodeEnum;
import me.luliru.gateway.core.exception.ApiBizException;
import me.luliru.gateway.core.exception.OpenGatewayException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * OrderMappingManager
 * Created by luliru on 2019/7/9.
 */
@Component
public class OrderMappingManager {

    @Resource
    private OrderMappingProvider orderMappingProvider;

    @Value("${remote.platform_id}")
    private Integer qaapiPlatformId;

    public int getCityIdByOrderOriginalId(String order_original_id){
        return fetchOrderMapping(order_original_id).getOrderMapping().getCityId();
    }

    public OrderMappingResponse fetchOrderMapping(String order_original_id){
        QueryRequest request = QueryRequest
                .create()
                .setOuterOrderId(order_original_id)
                .setPlatformId(qaapiPlatformId);
        OrderMappingResponse orderMappingResponse = orderMappingProvider.find(request);
        if("ORDER_MAPPING_NOT_EXIST".equals(orderMappingResponse.getRespName())){
            throw new ApiBizException(SubCodeEnum.ORDER_NOT_EXISTED);
        }
        if(!orderMappingResponse.isSuccess()){
            throw new OpenGatewayException(CodeEnum.API_UNKNOWN_ERROR);
        }
        return orderMappingResponse;
    }
}
