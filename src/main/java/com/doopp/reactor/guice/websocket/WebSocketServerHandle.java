package com.doopp.reactor.guice.websocket;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;

public interface WebSocketServerHandle {

    String secWebSocketProtocol(HttpServerRequest request);

    Mono<Void> onConnect(Channel channel);

    Mono<Void> handleEvent(WebSocketFrame frame, Channel channel);

    Mono<Void> onClose(CloseWebSocketFrame frame, Channel channel);

    Mono<Void> onError(Channel channel, Throwable error);
}
