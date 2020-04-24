package me.luliru.gateway.core.apiprocessor;

import me.luliru.gateway.core.RequestContext;
import reactor.core.publisher.Mono;

/**
 * TestApiProcessor
 * Created by luliru on 2019/7/8.
 */
public interface TestApiProcessor<T> {

    Mono<T> process(RequestContext ctx, TestApiContext context, String biz_params);
}
