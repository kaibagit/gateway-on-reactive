package me.luliru.gateway.core.gateway.v1;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import me.luliru.gateway.core.apiprocessor.TestOrderAcceptProcessor;
import me.luliru.gateway.core.netty.NettySharing;
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

        // V1编码固定流程
        GatewayInboundHandler gatewayInboundHandler = new GatewayInboundHandler(blockScheduler);
        gatewayInboundHandler.registeTestApiProcessor("test.order.accept",testOrderAcceptProcessor);

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

    public void  destroy(){
    }
}
