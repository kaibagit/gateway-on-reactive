package me.luliru.gateway.core.process.processor;

import me.luliru.gateway.core.RequestContext;
import me.luliru.gateway.core.process.Processor;
import me.luliru.gateway.core.process.ProcessorChain;
import me.luliru.gateway.core.process.dto.RequestParam;
import me.luliru.gateway.core.apiprocessor.TestOrderAcceptProcessor;
import me.luliru.gateway.core.repository.entity.ApplicationQa;
import reactor.core.publisher.Mono;

/**
 * ApiInvokeDispatchProcessor
 * Created by luliru on 2019/7/30.
 */
public class ApiInvokeDispatchProcessor implements Processor<ApplicationQa,ApplicationQa> {

    private TestOrderAcceptProcessor testOrderAcceptProcessor;

    public ApiInvokeDispatchProcessor(TestOrderAcceptProcessor testOrderAcceptProcessor){
        this.testOrderAcceptProcessor= testOrderAcceptProcessor;
    }

    @Override
    public Mono<ApplicationQa> process(RequestContext ctx, Mono<ApplicationQa> mono) {
        return mono.flatMap(applicationQa ->{
            RequestParam requestParam = (RequestParam) ctx.get("requestParam");
            ProcessorChain chain = ctx.chain();
            if(isSelfTestingApi(requestParam.getApi())){
                SelfTestingApiProcessor selfTestingApiProcessor = new SelfTestingApiProcessor();
                selfTestingApiProcessor.registeTestApiProcessor("test.order.accept",testOrderAcceptProcessor);
                chain.addLast(selfTestingApiProcessor);
            }else{
                chain.addLast(new OpenApiProcessor());
            }
            chain.addLast(new ExceptionHandleProcess());
            chain.addLast(new WriteProcessor());
            return Mono.just(applicationQa);
        });
    }

    private boolean isSelfTestingApi(String api){
        if(api.indexOf("test.") == 0){
            return true;
        }
        return false;
    }
}
