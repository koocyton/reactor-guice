package com.doopp.reactor.guice.websocket;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import reactor.core.publisher.Flux;

public interface WebSocketServerHandle {

    void connected(Channel channel);

    void connected(Channel channel, String channelKey);

    void sendTextMessage(String text, Channel channel);

    void sendTextMessage(String text, String channelKey);

    Flux<String> receiveTextMessage(Channel channel);

    void onTextMessage(TextWebSocketFrame frame, Channel channel);

    void onBinaryMessage(BinaryWebSocketFrame frame, Channel channel);

    void onPingMessage(PingWebSocketFrame frame, Channel channel);

    void onPongMessage(PongWebSocketFrame frame, Channel channel);

    void disconnect(Channel channel);
}
