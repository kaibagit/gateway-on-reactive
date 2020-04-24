package me.luliru.gateway.core.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.concurrent.DefaultPromise;
import lombok.extern.slf4j.Slf4j;
import me.luliru.gateway.core.enums.CodeEnum;
import me.luliru.gateway.core.exception.OpenGatewayException;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;

/**
 * HttpClient
 * Created by luliru on 2019/7/9.
 */
@Slf4j
public class HttpClient {

    private static Class channelClass;

    static {
        channelClass = NioSocketChannel.class;
        if(Epoll.isAvailable()){
            channelClass = EpollSocketChannel.class;
        }else if(KQueue.isAvailable()){
            channelClass = KQueueSocketChannel.class;
        }
    }

    /**
     * 不支持https
     * @param url
     * @param body
     * @return
     */
    public Mono<String> post(EventLoop currentEventLoop,String url,String body) {
        DefaultPromise<String> responsePromise = new DefaultPromise(currentEventLoop);

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(currentEventLoop);
        bootstrap.channel(channelClass);
        bootstrap.handler(new ChannelInitializer() {
            @Override
            public void initChannel(Channel ch) throws Exception {
                ch.pipeline().addLast(new HttpClientCodec());
                ch.pipeline().addLast(new HttpObjectAggregator(65536,true));
                ch.pipeline().addLast(new HttpClientInboundHandler(responsePromise));
            }
        });

        try{
            URI uri = new URI(url);

            Mono<Channel> channelMono =  Mono.create(sink -> {
                ChannelFuture channelFuture = bootstrap.connect(uri.getHost(), uri.getPort() < 0 ? 80 : uri.getPort());
                channelFuture.addListener(future -> {
                    if(future.isSuccess()){
                        sink.success(channelFuture.channel());
                    }else{
                        sink.error(future.cause());
                    }
                });
            });

            return channelMono.flatMap(channel -> {
                Mono<String> mono = Mono.create(sink -> {
                    DefaultFullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, uri.toASCIIString(), Unpooled.wrappedBuffer(body.getBytes()));
                    request.headers().set(HttpHeaderNames.HOST, uri.getHost());
                    request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
                    request.headers().set(HttpHeaderNames.CONTENT_LENGTH, request.content().readableBytes());
                    channel.writeAndFlush(request).addListener(future -> {
                        if (future.isSuccess()) {
                            responsePromise.addListener(responseFuture ->{
                                if(responseFuture.isSuccess()){
                                    sink.success((String) responseFuture.getNow());
                                }else{
                                    sink.error(responseFuture.cause());
                                }
                            });
                        } else {
                            sink.error(future.cause());
                        }
                    });
                });
                return mono;
            });
        }catch (URISyntaxException e){
            return Mono.error(e);
        }
    }

    class HttpClientInboundHandler extends ChannelInboundHandlerAdapter{

        private DefaultPromise promise;

        public HttpClientInboundHandler(DefaultPromise responsePromise){
            this.promise = responsePromise;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            log.debug("channelRead fired : {}",msg.getClass());

            if(msg instanceof FullHttpResponse){
                FullHttpResponse response = (FullHttpResponse) msg;
                if(response.status().code() != 200){
                    log.info("remote gateway reresponse http code : {}",response.status().code());
                    promise.setFailure(new OpenGatewayException(CodeEnum.SYS_UNKNOWN_ERROR));
                    ctx.channel().close();
                }
                String result = response.content().toString(Charset.defaultCharset());
                log.info("remote gateway response : {}",result);
                promise.setSuccess(result);
            }

//            if (msg instanceof HttpResponse) {
//                HttpResponse response = (HttpResponse) msg;
//                if(response.status().code() != 200){
//                    log.info("remote gateway reresponse http code : {}",response.status().code());
//                    promise.setFailure(new OpenGatewayException(CodeEnum.SYS_UNKNOWN_ERROR));
//                }
//            }
//
//            if (msg instanceof HttpContent) {
//                if (msg instanceof LastHttpContent){
//                    log.debug("response end.");
//                    ctx.close();
//                    return;
//                }
//
//                HttpContent httpContent = (HttpContent) msg;
//                ByteBuf content = httpContent.content();
//                String result = content.toString(Charset.defaultCharset());
//                content.release();
//                log.info("remote gateway response : {}",result);
//                promise.setSuccess(result);
//            }
        }
    }
}
