package me.luliru.gateway.core.process;

import io.netty.handler.codec.http.FullHttpRequest;
import me.luliru.gateway.core.RequestContext;

/**
 * ProcessorInitializer
 * Created by luliru on 2019/7/30.
 */
public interface ProcessorInitializer {

    void init(RequestContext ct, FullHttpRequest request);
}
