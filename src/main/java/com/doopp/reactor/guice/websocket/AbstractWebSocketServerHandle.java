package com.doopp.reactor.guice.websocket;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.*;
import reactor.netty.http.server.HttpServerRequest;

public abstract class AbstractWebSocketServerHandle implements WebSocketServerHandle {

    @Override
    public String secWebSocketProtocol(HttpServerRequest request) {
        return null;
    }

    @Override
    public void onConnect(Channel channel) {
        // System.out.println("onConnect" + channel);
        // RequestAttribute requestAttribute = channel.attr(RequestAttribute.REQUEST_ATTRIBUTE).get();
        // return Mono.empty();
    }

    @Override
    public void onError(Channel channel, Throwable error) {
        // System.out.println("onError" + channel);
        // channel.writeAndFlush(new TextWebSocketFrame(error.getMessage()));
    }

    @Override
    public void handleEvent(WebSocketFrame frame, Channel channel) {
        try {
            if (frame instanceof TextWebSocketFrame) {
                this.onTextMessage((TextWebSocketFrame) frame, channel);
            } else if (frame instanceof BinaryWebSocketFrame) {
                this.onBinaryMessage((BinaryWebSocketFrame) frame, channel);
            } else if (frame instanceof PingWebSocketFrame) {
                this.onPingMessage((PingWebSocketFrame) frame, channel);
            } else if (frame instanceof PongWebSocketFrame) {
                this.onPongMessage((PongWebSocketFrame) frame, channel);
            } else if (frame instanceof CloseWebSocketFrame) {
                this.onClose((CloseWebSocketFrame) frame, channel);
            }
        }
        catch (Exception e) {
            this.onError(channel, e);
        }
    }

    @Override
    public void onClose(CloseWebSocketFrame frame, Channel channel) {
        // System.out.println("onClose" + channel);
        if (channel.isOpen() && channel.isActive()) {
            channel.close();
        }
    }

    protected void onTextMessage(TextWebSocketFrame frame, Channel channel) {
        // channel.writeAndFlush(frame);
    }

    protected void onBinaryMessage(BinaryWebSocketFrame frame, Channel channel) {
        // channel.writeAndFlush(frame);
    }

    protected void onPingMessage(PingWebSocketFrame frame, Channel channel) {
        this.sendPongMessage(channel);
    }

    protected void onPongMessage(PongWebSocketFrame frame, Channel channel) {
        this.sendPingMessage(channel);
    }

    final protected void sendTextMessage(String text, Channel channel) {
        sendMessage(new TextWebSocketFrame(text), channel);
    }

    final protected void sendBinaryMessage(ByteBuf byteBuf, Channel channel) {
        sendMessage(new BinaryWebSocketFrame(byteBuf), channel);
    }

    final protected void sendBinaryMessage(byte[] bytes, Channel channel) {
        sendBinaryMessage(Unpooled.wrappedBuffer(bytes), channel);
    }

    final protected void sendPingMessage(Channel channel) {
        sendMessage(new PingWebSocketFrame(), channel);
    }

    final protected void sendPongMessage(Channel channel) {
        sendMessage(new PongWebSocketFrame(), channel);
    }

    private void sendMessage(WebSocketFrame frame, Channel channel) {
        channel.writeAndFlush(frame);
    }
}
