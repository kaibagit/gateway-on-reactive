package me.luliru.gateway.core.process;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpUtil;
import lombok.extern.slf4j.Slf4j;
import me.luliru.gateway.core.RequestContext;
import reactor.core.publisher.Mono;

/**
 * DispatchInboundHandler
 * Created by luliru on 2019/7/30.
 */
@Slf4j
public class ProcessorChain extends SimpleChannelInboundHandler<FullHttpRequest> {

    private ProcessorInitializer initializer;

    private ProcessorHolder head;

    private ProcessorHolder tail;

    private ProcessorHolder point;

    private Mono<?> mono;

    public ProcessorChain(ProcessorInitializer initializer){
        this.initializer = initializer;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        RequestContext rctx = RequestContext.createContext(ctx);
        rctx.setKeepAlive(HttpUtil.isKeepAlive(msg));
        rctx.chain(this);
        log.debug("channelRead0:{}",rctx.id());
        initializer.init(rctx,msg);

        point = head;
        mono = Mono.just(msg);

        loop(rctx);
    }

    private void loop(RequestContext rctx){
        if(point != null){
            Processor processor = point.getProcessor();
            try{
                mono = processor.process(rctx,mono);
                if(mono != null){
                    // 由于mono的机制，只有在subscribe时，事件才会被处理
                    // 所以必须在返回每个mono之后，通过调用subscribe，强制触发processor处理，从而实现processor动态增减
                    mono.subscribe(t -> {
                        log.debug("{} => {}",processor.getClass(),t);
                        mono = Mono.justOrEmpty(t);
                        point = point.getNext();
                        loop(rctx);
                    },e ->{
                        log.debug("{} => exception",processor.getClass(),e);
                        mono = Mono.error(e);
                        point = point.getNext();
                        loop(rctx);
                    });
                }
            }catch (Exception e){
                log.warn("出现未知异常",e);
            }
        }
    }

    public void addLast(Processor processor){
        addLast(processor.toString(),processor);
    }

    public void addLast(String name,Processor processor){
        ProcessorHolder holder = new ProcessorHolder();
        holder.setName(processor.toString());
        holder.setProcessor(processor);
        if(head == null){
            head = holder;
            tail = holder;
        }else{
            tail.setNext(holder);
            holder.setPrev(tail);
            tail = holder;
        }
    }

    public void channelInactive(ChannelHandlerContext ctx) {
        RequestContext.destroyContext(ctx);
        ctx.fireChannelInactive();
    }
}
