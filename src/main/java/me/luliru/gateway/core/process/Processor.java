package me.luliru.gateway.core.process;

import me.luliru.gateway.core.RequestContext;
import reactor.core.publisher.Mono;

/**
 * Processor
 * Created by luliru on 2019/7/26.
 */
public interface Processor<T,R> {

    /**
     * 处理，如果中断处理，则返回null;最后一个Processor需要返回null
     * @param ctx
     * @param mono
     * @return
     */
    Mono<R> process(RequestContext ctx, Mono<T> mono);
}
