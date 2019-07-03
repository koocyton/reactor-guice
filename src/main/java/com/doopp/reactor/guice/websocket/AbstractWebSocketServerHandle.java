package com.doopp.reactor.guice.websocket;

import com.doopp.reactor.guice.RequestAttribute;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.*;
import reactor.core.publisher.Mono;

public abstract class AbstractWebSocketServerHandle implements WebSocketServerHandle {

    @Override
    public Mono<Void> onConnect(Channel channel) {
        RequestAttribute requestAttribute = channel.attr(RequestAttribute.REQUEST_ATTRIBUTE).get();
        return Mono.empty();
    }

    @Override
    public Mono<Void> onError(Channel channel, Throwable error) {
        channel.writeAndFlush(new TextWebSocketFrame(error.getMessage()));
        return this.onClose(null, channel);
    }

    @Override
    public Mono<Void> handleEvent(WebSocketFrame frame, Channel channel) {
        try {
            if (frame instanceof TextWebSocketFrame) {
                return this.onTextMessage((TextWebSocketFrame) frame, channel);
            } else if (frame instanceof BinaryWebSocketFrame) {
                return this.onBinaryMessage((BinaryWebSocketFrame) frame, channel);
            } else if (frame instanceof PingWebSocketFrame) {
                return this.onPingMessage((PingWebSocketFrame) frame, channel);
            } else if (frame instanceof PongWebSocketFrame) {
                return this.onPongMessage((PongWebSocketFrame) frame, channel);
            } else if (frame instanceof CloseWebSocketFrame) {
                return this.onClose((CloseWebSocketFrame) frame, channel);
            }
        }
        catch (Exception e) {
            return this.onError(channel, e);
        }
        return Mono.empty();
    }

    @Override
    public Mono<Void> onClose(CloseWebSocketFrame frame, Channel channel) {
        if (channel.isOpen() && channel.isActive()) {
            channel.close();
        }
        return Mono.empty();
    }

    protected Mono<Void> onTextMessage(TextWebSocketFrame frame, Channel channel) {
        // channel.writeAndFlush(frame);
        return Mono.empty();
    }

    protected Mono<Void> onBinaryMessage(BinaryWebSocketFrame frame, Channel channel) {
        // channel.writeAndFlush(frame);
        return Mono.empty();
    }

    protected Mono<Void> onPingMessage(PingWebSocketFrame frame, Channel channel) {
        return this.sendPongMessage(channel);
    }

    protected Mono<Void> onPongMessage(PongWebSocketFrame frame, Channel channel) {
        return this.sendPingMessage(channel);
    }

    final protected Mono<Void> sendTextMessage(String text, Channel channel) {
        return sendMessage(new TextWebSocketFrame(text), channel);
    }

    final protected Mono<Void> sendBinaryMessage(ByteBuf byteBuf, Channel channel) {
        return sendMessage(new BinaryWebSocketFrame(byteBuf), channel);
    }

    final protected Mono<Void> sendBinaryMessage(byte[] bytes, Channel channel) {
        return sendBinaryMessage(Unpooled.wrappedBuffer(bytes), channel);
    }

    final protected Mono<Void> sendPingMessage(Channel channel) {
        return sendMessage(new PingWebSocketFrame(), channel);
    }

    final protected Mono<Void> sendPongMessage(Channel channel) {
        return sendMessage(new PongWebSocketFrame(), channel);
    }

    private Mono<Void> sendMessage(WebSocketFrame frame, Channel channel) {
        channel.writeAndFlush(frame);
        return Mono.empty();
    }
}
