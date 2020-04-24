package me.luliru.gateway.core.mock.v3;

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
import me.luliru.gateway.core.mock.v1.MockTestApiProcessor;
import me.luliru.gateway.core.mock.v2.MockApplicationQaFindProcessor;
import me.luliru.gateway.core.mock.v2.MockValidateProcessor;
import me.luliru.gateway.core.process.ProcessorChain;
import me.luliru.gateway.core.process.ProcessorInitializer;
import me.luliru.gateway.core.process.processor.ParamsFetchProcessor;
import reactor.core.scheduler.Scheduler;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * MockGatewayV3
 * Created by luliru on 2019/8/5.
 */
//@Component
public class MockGatewayV3 {

    @Resource
    private TestOrderAcceptProcessor testOrderAcceptProcessor;

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

        // V3动态变更流程
        // 支持分支，支持动态增减processor
        ProcessorInitializer processorInitializer = new ProcessorInitializer() {
            @Override
            public void init(RequestContext ct, FullHttpRequest request) {
                ProcessorChain chain = ct.chain();
                chain.addLast(new ParamsFetchProcessor());
                chain.addLast(new MockApplicationQaFindProcessor(blockScheduler));
                chain.addLast(new MockValidateProcessor());
                chain.addLast(new MockApiInvokeDispatchProcessor(mockTestApiProcessor));
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
                .option(ChannelOption.SO_BACKLOG, 2000)
                .childOption(ChannelOption.SO_KEEPALIVE, false);

        ChannelFuture f = b.bind(15295).sync();
    }
}
