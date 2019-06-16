package com.doopp.reactor.guice.publisher;

import com.doopp.reactor.guice.RequestAttribute;
import com.doopp.reactor.guice.websocket.WebSocketServerHandle;
import io.netty.channel.Channel;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ReplayProcessor;
import reactor.netty.NettyPipeline;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

public class WebsocketPublisher {

    public Mono<Object> sendMessage(HttpServerRequest request, HttpServerResponse response, WebSocketServerHandle handleObject, Object requestAttribute) {
        return Mono.just((RequestAttribute) requestAttribute)
            .flatMap(r ->
                response.header("content-type", "text/plain")
                    .sendWebsocket((in, out) ->
                        out.withConnection(connect -> {
                            // channel
                            Channel channel = connect.channel();
                            // on disconnect
                            connect.onDispose().subscribe(null, null, () ->
                                handleObject.onClose(null, channel)
                            );
                            // set requestAttribute to channel
                            channel.attr(RequestAttribute.REQUEST_ATTRIBUTE).set(r);
                            // set channel to requestAttribute
                            // requestAttribute.setAttribute(CURRENT_CHANNEL, channel);
                            // on connect
                            handleObject.onConnect(channel);
                            // receive frame
                            in.aggregateFrames().receiveFrames().subscribe(frame ->
                                handleObject.handleEvent(frame, channel)
                            );
                        })
                        // options
                        .options(NettyPipeline.SendOptions::flushOnEach)
                        // send string
                        .sendString(
                            ReplayProcessor.create()
                            // on send message
                            // handleObject.receiveTextMessage(
                            //        r.getAttribute(CURRENT_CHANNEL, Channel.class)
                            //)
                        )
                    )
            );
    }
}
