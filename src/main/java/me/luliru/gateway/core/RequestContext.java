package me.luliru.gateway.core;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoop;
import lombok.extern.slf4j.Slf4j;
import me.luliru.gateway.core.process.ProcessorChain;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * RequestContext
 * Created by luliru on 2019/7/25.
 */
@Slf4j
public class RequestContext<T> {

    private String id;

    private EventLoop eventLoop;

    private Scheduler scheduler;

    private ChannelHandlerContext channelHandlerCtx;

    private ProcessorChain chain;

    private Map<String,Object> attachment = new HashMap<>();

    private boolean keepAlive;

    private static ThreadLocal<Map<String,RequestContext>> contextMapLocal = new ThreadLocal<>();

    /**
     * 该方法只能被Netty EventLoop调用
     * @param channelHandlerCtx
     * @return
     */
    public static RequestContext createContext(ChannelHandlerContext channelHandlerCtx){
        RequestContext ctx = new RequestContext(channelHandlerCtx);
        Map<String,RequestContext> ctxMap = contextMapLocal.get();
        if(ctxMap == null){
            ctxMap = new HashMap<>();
            contextMapLocal.set(ctxMap);
        }
        ctxMap.put(ctx.id,ctx);
        return ctx;
    }

    public static void destroyContext(ChannelHandlerContext channelHandlerCtx){
        Map<String,RequestContext> ctxMap = contextMapLocal.get();
        if(ctxMap != null){
            ctxMap.remove(channelHandlerCtx);
            log.debug("destroyContext");
        }
    }

    public static void destroyContext(ChannelHandlerContext channelHandlerCtx,RequestContext ctx){
        Map<String,RequestContext> ctxMap = contextMapLocal.get();
        if(ctxMap != null){
            ctxMap.remove(ctx.id);
        }
    }

    public RequestContext(ChannelHandlerContext channelHandlerCtx){
        Channel channel = channelHandlerCtx.channel();
        this.channelHandlerCtx = channelHandlerCtx;
        id = UUID.randomUUID().toString().replace("-","");
        eventLoop = channel.eventLoop();
        scheduler = Schedulers.fromExecutor(eventLoop);
    }

    public void chain(ProcessorChain chain){
        this.chain = chain;
    }

    public Mono<T> next(Object msg){
        return null;
    }

    public String id(){
        return id;
    }

    public EventLoop eventLoop(){
        return eventLoop;
    }

    public Scheduler scheduler(){
        return scheduler;
    }

    public void put(String key,Object val){
        attachment.put(key,val);
    }

    public Object get(String key){
        return attachment.get(key);
    }

    public ChannelHandlerContext getChannelHandlerCtx() {
        return channelHandlerCtx;
    }

    public ProcessorChain chain() {
        return chain;
    }

    public boolean isKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }
}
