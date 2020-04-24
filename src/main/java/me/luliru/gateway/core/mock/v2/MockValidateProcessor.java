package me.luliru.gateway.core.mock.v2;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import me.luliru.gateway.core.RequestContext;
import me.luliru.gateway.core.enums.CodeEnum;
import me.luliru.gateway.core.exception.OpenGatewayException;
import me.luliru.gateway.core.process.Processor;
import me.luliru.gateway.core.process.dto.RequestParam;
import me.luliru.gateway.core.repository.entity.ApplicationQa;
import me.luliru.gateway.core.util.Signer;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;

/**
 * MockValidateProcessor
 * Created by luliru on 2019/8/5.
 */
@Slf4j
public class MockValidateProcessor implements Processor<ApplicationQa,ApplicationQa> {

    private String qaAccessToken = "TEST2018-a444-4e50-b785-f48ba984bd9c";

    @Override
    public Mono<ApplicationQa> process(RequestContext ctx, Mono<ApplicationQa> mono) {
        return mono.flatMap(applicationQa -> {
            log.info("【{}】 处理applicationQa",ctx.id());
            RequestParam requestParam = (RequestParam) ctx.get("requestParam");
            log.info("biz_param:{}",requestParam.getBiz_param());
            // 验签
            String generatedSign = Signer.sign(requestParam.getAppkey(), requestParam.getTimestampStr(), requestParam.getNonce(), requestParam.getAccess_token(), requestParam.getApi(),applicationQa.getAppSecret(),requestParam.getBiz_param());

            // 验证access_token
            boolean needAccessToken = needAccessToken(requestParam.getApi());
            if(needAccessToken){
                if(requestParam.getAccess_token() == null){
                    return Mono.error(new OpenGatewayException(CodeEnum.SYS_MISSING_PARAMETER,"access_token不能为空"));
                }
                if(StringUtils.isBlank(requestParam.getAccess_token())){
                    return Mono.error(new OpenGatewayException(CodeEnum.SYS_INVALID_PARAMETER,"access_token不能为空"));
                }
                if(!qaAccessToken.equals(requestParam.getAccess_token())){
                    return Mono.error(new OpenGatewayException(CodeEnum.SYS_INVALID_PARAMETER,"无效的access_token"));
                }
            }

            // qa环境特殊业务参数校验
            validateBizParams(requestParam.getAppkey(), requestParam.getApi(),requestParam.getBiz_param());
            return Mono.just(applicationQa);
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
}
