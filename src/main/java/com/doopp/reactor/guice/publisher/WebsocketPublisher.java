package com.doopp.reactor.guice.publisher;

import com.doopp.reactor.guice.RequestAttribute;
import com.doopp.reactor.guice.websocket.WebSocketServerHandle;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.*;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.netty.Connection;
import reactor.netty.NettyPipeline;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import reactor.netty.http.websocket.WebsocketInbound;
import reactor.netty.http.websocket.WebsocketOutbound;

public class WebsocketPublisher {

    private static final String CURRENT_CHANNEL = "current_channel";

    public Mono<Object> sendMessage(HttpServerRequest request, HttpServerResponse response, WebSocketServerHandle handleObject, Object requestAttribute) {
        return Mono
                .just((RequestAttribute) requestAttribute)
                .flatMap(r ->
                        response.header("content-type", "text/plain")
                                .sendWebsocket((in, out) ->
                                        this.websocketPublisher(in, out, handleObject, r)
                                )
                );
    }

    private Publisher<Void> websocketPublisher(WebsocketInbound in, WebsocketOutbound out, WebSocketServerHandle handleObject, RequestAttribute requestAttribute) {
        return out.withConnection(connect -> {
            this.onConnected(in, connect, handleObject, requestAttribute);
        })
            // options
            .options(NettyPipeline.SendOptions::flushOnEach)
            // send string
            .sendString(
                // on send message
                handleObject.receiveTextMessage(
                    requestAttribute.getAttribute(CURRENT_CHANNEL, Channel.class)
                )
            );
    }

    private void onConnected(WebsocketInbound in, Connection connect, WebSocketServerHandle handleObject, RequestAttribute requestAttribute) {
        // channel
        Channel channel = connect.channel();
        // on disconnect
        connect.onDispose().subscribe(null, null, () -> {
            handleObject.disconnect(channel);
        });
        // set requestAttribute to channel
        channel.attr(RequestAttribute.REQUEST_ATTRIBUTE).set(requestAttribute);
        // set channel to requestAttribute
        requestAttribute.setAttribute(CURRENT_CHANNEL, channel);
        // on connect
        handleObject.connected(channel);
        // on receive
        in.aggregateFrames().receiveFrames().flatMap(frame -> {
            // text frame
            if (frame instanceof TextWebSocketFrame) {
                handleObject.onTextMessage((TextWebSocketFrame) frame, channel);
            }
            // binary frame
            else if (frame instanceof BinaryWebSocketFrame) {
                handleObject.onBinaryMessage((BinaryWebSocketFrame) frame, channel);
            }
            // ping frame
            else if (frame instanceof PingWebSocketFrame) {
                handleObject.onPingMessage((PingWebSocketFrame) frame, channel);
            }
            // pong frame
            else if (frame instanceof PongWebSocketFrame) {
                handleObject.onPongMessage((PongWebSocketFrame) frame, channel);
            }
            // close ?
            else if (frame instanceof CloseWebSocketFrame) {
                connect.dispose();
                handleObject.disconnect(channel);
            }
            return Mono.empty();
        })
            .onErrorResume(throwable -> {
                handleObject.disconnect(channel);
                return Mono.error(throwable);
            })
            .subscribe();
    }
}
