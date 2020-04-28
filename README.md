# gateway-on-reactive

一个基于react模式的全异步网关sample，底层技术采用了netty和reactive。

## 设计思路

1、 为了解决异步化的动态扩展和编排问题，参考了netty的pipeline思路，设计了`Processor`和`ProcessorChain`组件，类似于netty的`ChannelHandler`和`Pipeline`。

2、 为了解决异步化引发的callback hell文件，引入了reactive框架。

3、 整个系统涉及如下2组线程池：

1)  accept线程，即netty的EventLoop线程，用于接受新连接。

2）核心线程，即netty的EventLoop线程，主要用于非阻塞存CPU计算一类的工作，如网络IO读写、序列化/反序列化等操作。

3）阻塞线程，即用来处理会阻塞线程的操作，如：磁盘文件读写、JDBC方式操作数据库等。

## 使用示例

1、扩展核心接口：

```java
public interface Processor<T,R> {

    /**
     * 处理，如果中断处理，则返回null;最后一个Processor需要返回null
     * @param ctx
     * @param mono
     * @return
     */
    Mono<R> process(RequestContext ctx, Mono<T> mono);
}
```

2、编排之后启动netty：

```java
    Class channelClass = NioServerSocketChannel.class;
    if(Epoll.isAvailable()){
        channelClass = EpollServerSocketChannel.class;
    }else if(KQueue.isAvailable()){
        channelClass = KQueueServerSocketChannel.class;
    }

    EventLoopGroup acceptGroup = NettySharing.acceptGroup();
    EventLoopGroup eventLoopGroup = NettySharing.ioWorkerGroup();

    StaticProcessorsHandler staticProcessorsHandler = new StaticProcessorsHandler();
    staticProcessorsHandler.addProcessor(new ParamsFetchProcessor());
    staticProcessorsHandler.addProcessor(new ApplicationQaFindProcessor(blockScheduler));
    staticProcessorsHandler.addProcessor(new ValidateProcessor());
    staticProcessorsHandler.addProcessor(apiInvokeProcessor);
    staticProcessorsHandler.addProcessor(new ExceptionHandleProcess());
    staticProcessorsHandler.addProcessor(new WriteProcessor());

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
```