package me.luliru.gateway.core.gateway.v3;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import me.luliru.gateway.core.RequestContext;
import me.luliru.gateway.core.apiprocessor.TestOrderAcceptProcessor;
import me.luliru.gateway.core.netty.NettySharing;
import me.luliru.gateway.core.process.ProcessorChain;
import me.luliru.gateway.core.process.ProcessorInitializer;
import me.luliru.gateway.core.process.processor.ApiInvokeDispatchProcessor;
import me.luliru.gateway.core.process.processor.ApplicationQaFindProcessor;
import me.luliru.gateway.core.process.processor.ParamsFetchProcessor;
import me.luliru.gateway.core.process.processor.ValidateProcessor;
import org.springframework.stereotype.Component;
import reactor.core.scheduler.Scheduler;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * Gateway
 * Created by luliru on 2019/7/8.
 */
//@Component
public class Gateway {

    @Resource
    private TestOrderAcceptProcessor testOrderAcceptProcessor;

    @Resource(name="blockScheduler")
    private Scheduler blockScheduler;

    @PostConstruct
    public void init() throws InterruptedException {
        Class channelClass = NioServerSocketChannel.class;
        if(Epoll.isAvailable()){
            channelClass = EpollServerSocketChannel.class;
        }else if(KQueue.isAvailable()){
            channelClass = KQueueServerSocketChannel.class;
        }

        EventLoopGroup acceptGroup = NettySharing.acceptGroup();
        EventLoopGroup eventLoopGroup = NettySharing.ioWorkerGroup();

        // V3动态变更流程
        // 支持分支，支持动态增减processor
        ProcessorInitializer processorInitializer = new ProcessorInitializer() {
            @Override
            public void init(RequestContext ct, FullHttpRequest request) {
                ProcessorChain chain = ct.chain();
                chain.addLast(new ParamsFetchProcessor());
                chain.addLast(new ApplicationQaFindProcessor(blockScheduler));
                chain.addLast(new ValidateProcessor());
                chain.addLast(new ApiInvokeDispatchProcessor(testOrderAcceptProcessor));
//                chain.addLast(apiInvokeProcessor);
//                chain.addLast(new ExceptionHandleProcess());
//                chain.addLast(new WriteProcessor());
            }
        };

        ServerBootstrap b = new ServerBootstrap();
        b.group(acceptGroup, eventLoopGroup).channel(channelClass)
                .childHandler(new ChannelInitializer() {
                    @Override
                    public void initChannel(Channel ch) throws Exception {
                        ch.pipeline().addLast(new HttpServerCodec());
                        ch.pipeline().addLast(new HttpObjectAggregator(65536,true));
                        ch.pipeline().addLast(new ProcessorChain(processorInitializer));
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true);

        ChannelFuture f = b.bind(15295).sync();
    }

    public void  destroy(){
    }
}
