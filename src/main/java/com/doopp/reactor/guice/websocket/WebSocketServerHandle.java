package com.doopp.reactor.guice.websocket;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

public interface WebSocketServerHandle {

    void onConnect(Channel channel);

    void handleEvent(WebSocketFrame frame, Channel channel);

    void onClose(CloseWebSocketFrame frame, Channel channel);

    void onError(Channel channel, Throwable error);
}
