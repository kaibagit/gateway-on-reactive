package me.luliru.gateway.core.mock.v1;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import me.luliru.gateway.core.netty.NettySharing;
import org.springframework.stereotype.Component;
import reactor.core.scheduler.Scheduler;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * MockGateway
 * Created by luliru on 2019/8/1.
 */
@Component
public class MockGateway {

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

        // V1编码固定流程
        MockGatewayInboundHandler gatewayInboundHandler = new MockGatewayInboundHandler(blockScheduler);
        gatewayInboundHandler.registeTestApiProcessor("test.order.arrive",mockTestApiProcessor);
        gatewayInboundHandler.registeTestApiProcessor("test.order.accept",mockTestApiProcessor);
        gatewayInboundHandler.registeTestApiProcessor("dianwoda.order.create",mockTestApiProcessor);

        ServerBootstrap b = new ServerBootstrap();
        b.group(acceptGroup, eventLoopGroup).channel(channelClass)
                .childHandler(new ChannelInitializer() {
                    @Override
                    public void initChannel(Channel ch) throws Exception {
                        ch.pipeline().addLast(new HttpServerCodec());
                        ch.pipeline().addLast(new HttpObjectAggregator(65536,true));
                        ch.pipeline().addLast(gatewayInboundHandler);
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true);

        ChannelFuture f = b.bind(15295).sync();
    }
}
