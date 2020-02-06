package com.doopp.reactor.guice.publisher;

import com.doopp.reactor.guice.RequestAttribute;
import com.doopp.reactor.guice.websocket.WebSocketServerHandle;
import io.netty.channel.Channel;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.UnicastProcessor;
import reactor.netty.NettyPipeline;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

public class WebsocketPublisher {

    // the out put
    private static Flux<Object> rp = UnicastProcessor.create();

    public Mono<Object> sendMessage(HttpServerRequest request, HttpServerResponse response, WebSocketServerHandle handleObject, Object requestAttributeObject) {

        return Mono.just((RequestAttribute) requestAttributeObject)
                .flatMap(requestAttribute ->
                        response.header("content-type", "text/plain")
                                .sendWebsocket((in, out) -> {
                                    // return
                                    return out.withConnection(
                                            connect -> {
                                                // channel
                                                Channel channel = connect.channel();
                                                System.out.println("\n\n >>>" + in);
                                                System.out.println(channel);
                                                // on disconnect
                                                connect.onDispose().subscribe(null, null, () -> {
                                                    // System.out.println(handleObject);
                                                    handleObject.onClose(null, channel);
                                                });
                                                // set requestAttribute to channel
                                                channel.attr(RequestAttribute.REQUEST_ATTRIBUTE).set(requestAttribute);
                                                // set channel to requestAttribute
                                                // requestAttribute.setAttribute(CURRENT_CHANNEL, channel);
                                                // on connect
                                                handleObject.onConnect(channel);
                                                // System.out.println("on connect");
                                                // receive frame
                                                in.aggregateFrames().receiveFrames().subscribe(
                                                        frame->handleObject.handleEvent(frame, channel)
                                                );
                                            })
                                            // options
                                            // .options(NettyPipeline.SendOptions::flushOnEach)
                                            // .sendObject(Flux.just("abc".getBytes()));
                                            .sendObject(rp);
                                })
                );
    }
}
