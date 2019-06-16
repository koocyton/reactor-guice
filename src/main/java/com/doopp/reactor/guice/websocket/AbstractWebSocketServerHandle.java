package com.doopp.reactor.guice.websocket;

import com.doopp.reactor.guice.RequestAttribute;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.*;

public abstract class AbstractWebSocketServerHandle implements WebSocketServerHandle {

    @Override
    public void onConnect(Channel channel) {
        RequestAttribute requestAttribute = channel.attr(RequestAttribute.REQUEST_ATTRIBUTE).get();
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
        if (channel.isOpen() && channel.isActive()) {
            channel.close();
        }
    }

    @Override
    public void onError(Channel channel, Throwable error) {
        channel.writeAndFlush(new TextWebSocketFrame(error.getMessage()));
        this.onClose(null, channel);
    }

    protected void sendTextMessage(TextWebSocketFrame frame, Channel channel) {
        channel.writeAndFlush(frame.retain());
    }

    protected void onTextMessage(TextWebSocketFrame frame, Channel channel) {
        // channel.writeAndFlush(frame);
    }

    protected void sendBinaryMessage(BinaryWebSocketFrame frame, Channel channel) {
        channel.writeAndFlush(frame.retain());
    }

    protected void onBinaryMessage(BinaryWebSocketFrame frame, Channel channel) {
        // channel.writeAndFlush(frame);
    }

    protected void sendPingMessage(PingWebSocketFrame frame, Channel channel) {
        channel.writeAndFlush(frame.retain());
    }

    protected void onPingMessage(PingWebSocketFrame frame, Channel channel) {
        this.sendPongMessage(new PongWebSocketFrame(), channel);
    }

    protected void sendPongMessage(PongWebSocketFrame frame, Channel channel) {
        channel.writeAndFlush(frame.retain());
    }

    protected void onPongMessage(PongWebSocketFrame frame, Channel channel) {
        this.sendPingMessage(new PingWebSocketFrame(), channel);
    }
}
