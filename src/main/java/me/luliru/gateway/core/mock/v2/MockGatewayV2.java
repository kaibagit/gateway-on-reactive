package me.luliru.gateway.core.mock.v2;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import me.luliru.gateway.core.gateway.v2.StaticProcessorsHandler;
import me.luliru.gateway.core.netty.NettySharing;
import me.luliru.gateway.core.mock.v1.MockTestApiProcessor;
import me.luliru.gateway.core.process.processor.ExceptionHandleProcess;
import me.luliru.gateway.core.process.processor.ParamsFetchProcessor;
import me.luliru.gateway.core.process.processor.WriteProcessor;
import reactor.core.scheduler.Scheduler;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * MockGatewayV2
 * Created by luliru on 2019/8/5.
 */
//@Component
public class MockGatewayV2 {

    @Resource
    private MockTestApiProcessor mockTestApiProcessor;

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

        // V2初始时才固定流程
        // 不支持分支判断，动态增减processor
        MockApiInvokeProcessor apiInvokeProcessor = new MockApiInvokeProcessor();
        apiInvokeProcessor.registeTestApiProcessor("test.order.arrive",mockTestApiProcessor);
        apiInvokeProcessor.registeTestApiProcessor("test.order.accept",mockTestApiProcessor);
        apiInvokeProcessor.registeTestApiProcessor("dianwoda.order.create",mockTestApiProcessor);
        StaticProcessorsHandler staticProcessorsHandler = new StaticProcessorsHandler();
        staticProcessorsHandler.getProcessorList().add(new ParamsFetchProcessor());
        staticProcessorsHandler.getProcessorList().add(new MockApplicationQaFindProcessor(blockScheduler));
        staticProcessorsHandler.getProcessorList().add(new MockValidateProcessor());
        staticProcessorsHandler.getProcessorList().add(apiInvokeProcessor);
        staticProcessorsHandler.getProcessorList().add(new ExceptionHandleProcess());
        staticProcessorsHandler.getProcessorList().add(new WriteProcessor());

        ServerBootstrap b = new ServerBootstrap();
        b.group(acceptGroup, eventLoopGroup).channel(channelClass)
                .childHandler(new ChannelInitializer() {
                    @Override
                    public void initChannel(Channel ch) throws Exception {
                        ch.pipeline().addLast(new HttpServerCodec());
                        ch.pipeline().addLast(new HttpObjectAggregator(65536,true));
                        ch.pipeline().addLast(staticProcessorsHandler);
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true);

        ChannelFuture f = b.bind(15295).sync();
    }
}
