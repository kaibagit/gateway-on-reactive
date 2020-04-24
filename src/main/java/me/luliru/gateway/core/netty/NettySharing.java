package me.luliru.gateway.core.netty;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

/**
 * Netty共享对象，主要是EventLoopGroup
 */
public class NettySharing {

    private static EventLoopGroup acceptGroup;

    private static EventLoopGroup ioWorkerGroup;    //默认是CPU核数的2倍

    static{
        if(Epoll.isAvailable()){
            EpollEventLoopGroup epollAcceptGroup = new EpollEventLoopGroup(1);
            epollAcceptGroup.setIoRatio(100);
            acceptGroup = epollAcceptGroup;
            ioWorkerGroup = new EpollEventLoopGroup();
        }else if(KQueue.isAvailable()){
            KQueueEventLoopGroup kqueueAcceptGroup = new KQueueEventLoopGroup(1);
            kqueueAcceptGroup.setIoRatio(100);
            acceptGroup = kqueueAcceptGroup;
            ioWorkerGroup = new KQueueEventLoopGroup();
        }else{
            NioEventLoopGroup nioAcceptGroup = new NioEventLoopGroup(1);
            nioAcceptGroup.setIoRatio(100);
            acceptGroup = nioAcceptGroup;
            ioWorkerGroup = new NioEventLoopGroup();
        }
    }

    public static EventLoopGroup acceptGroup(){
        return acceptGroup;
    }

    public static EventLoopGroup ioWorkerGroup(){
        return ioWorkerGroup;
    }
}
