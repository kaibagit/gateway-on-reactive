package me.luliru.gateway.core.process.processor;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import lombok.extern.slf4j.Slf4j;
import me.luliru.gateway.core.RequestContext;
import me.luliru.gateway.core.process.Processor;
import reactor.core.publisher.Mono;

import static io.netty.handler.codec.http.HttpHeaderValues.KEEP_ALIVE;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * WriteProcessor
 * Created by luliru on 2019/7/29.
 */
@Slf4j
public class WriteProcessor implements Processor<String,Void> {

    @Override
    public Mono<Void> process(RequestContext ctx, Mono<String> mono) {
        mono.subscribe(json -> {
            log.debug("【{}】 response writing.",ctx.id());
            byte[] data = json.getBytes();
            FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(data));
            response.headers().set("Content-Type", "application/json;charset=UTF-8");
            response.headers().set("Content-Length", Integer.toString(data.length));
            if(!ctx.isKeepAlive()){
                ctx.getChannelHandlerCtx().writeAndFlush(response).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        RequestContext.destroyContext(ctx.getChannelHandlerCtx(),ctx);
                        future.channel().close();
                    }
                });
            }else{
                response.headers().set(CONNECTION, KEEP_ALIVE);
                ctx.getChannelHandlerCtx().writeAndFlush(response).addListener(new ChannelFutureListener(){

                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        RequestContext.destroyContext(ctx.getChannelHandlerCtx(),ctx);
                    }
                });
            }
        });
        return null;
    }
}
