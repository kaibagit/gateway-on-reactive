package me.luliru.gateway.core.gateway.v2;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import lombok.extern.slf4j.Slf4j;
import me.luliru.gateway.core.RequestContext;
import me.luliru.gateway.core.process.Processor;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * DispatchInboundHandler
 * Created by luliru on 2019/7/29.
 */
@Slf4j
@ChannelHandler.Sharable
public class StaticProcessorsHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private List<Processor> processorList = new ArrayList<>();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        RequestContext rctx = RequestContext.createContext(ctx);
        log.debug("channelRead0:{}",rctx.id());
        Mono<?> mono = Mono.just(msg);
        for(Processor processor : processorList){
            if(mono == null){
                break;
            }
            try{
                mono = processor.process(rctx,mono);
            }catch (Exception e){
                log.error("出现未知异常",e);
                mono = null;
            }
        }
        if(mono != null){
            mono.subscribe(t ->{
                log.warn("有未处理的消息：{}",t);
            }, e -> {
                log.error("有未处理的异常",e);
            });
        }
    }

    public void channelInactive(ChannelHandlerContext ctx) {
        RequestContext.destroyContext(ctx);
        ctx.fireChannelInactive();
    }

    public void addProcessor(Processor processor) {
        processorList.add(processor);
    }
}
