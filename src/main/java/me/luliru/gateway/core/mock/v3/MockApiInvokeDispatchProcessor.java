package me.luliru.gateway.core.mock.v3;

import me.luliru.gateway.core.RequestContext;
import me.luliru.gateway.core.mock.v1.MockTestApiProcessor;
import me.luliru.gateway.core.mock.v2.MockApiInvokeProcessor;
import me.luliru.gateway.core.process.Processor;
import me.luliru.gateway.core.process.ProcessorChain;
import me.luliru.gateway.core.process.dto.RequestParam;
import me.luliru.gateway.core.process.processor.ExceptionHandleProcess;
import me.luliru.gateway.core.process.processor.WriteProcessor;
import me.luliru.gateway.core.repository.entity.ApplicationQa;
import reactor.core.publisher.Mono;

/**
 * MockApiInvokeDispatchProcessor
 * Created by luliru on 2019/8/5.
 */
public class MockApiInvokeDispatchProcessor implements Processor<ApplicationQa,ApplicationQa> {

    private MockTestApiProcessor testOrderAcceptProcessor;

    public MockApiInvokeDispatchProcessor(MockTestApiProcessor testOrderAcceptProcessor){
        this.testOrderAcceptProcessor= testOrderAcceptProcessor;
    }

    @Override
    public Mono<ApplicationQa> process(RequestContext ctx, Mono<ApplicationQa> mono) {
        return mono.flatMap(applicationQa ->{
            RequestParam requestParam = (RequestParam) ctx.get("requestParam");
            ProcessorChain chain = ctx.chain();

            MockApiInvokeProcessor selfTestingApiProcessor = new MockApiInvokeProcessor();
            selfTestingApiProcessor.registeTestApiProcessor("test.order.arrive",testOrderAcceptProcessor);
            selfTestingApiProcessor.registeTestApiProcessor("test.order.accept",testOrderAcceptProcessor);
            selfTestingApiProcessor.registeTestApiProcessor("dianwoda.order.create",testOrderAcceptProcessor);
            chain.addLast(selfTestingApiProcessor);
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
