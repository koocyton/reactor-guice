package com.doopp.reactor.guice.websocket;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.AttributeKey;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxProcessor;
import reactor.core.publisher.ReplayProcessor;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractWebSocketServerHandle implements WebSocketServerHandle {

    private Map<String, Channel> channelMap = new HashMap<>();

    private Map<String, FluxProcessor<String, String>> queueMessageMap = new HashMap<>();

    private Map<String, Channel[]> channelGroupMap = new HashMap<>();

    private static AttributeKey<String> CHANNEL_UNIQUE_KEY = AttributeKey.newInstance("channel_unique_key");

    @Override
    public void connected(Channel channel) {
        String channelKey = channel.id().asShortText();
        this.connected(channel, channelKey);
    }

    @Override
    public synchronized void connected(Channel channel, String channelKey) {
        channel.attr(CHANNEL_UNIQUE_KEY).set(channelKey);
        channelMap.put(channelKey, channel);
        queueMessageMap.put(channelKey, ReplayProcessor.create());
    }

    @Override
    public void sendTextMessage(String text, Channel channel) {
        // channel.writeAndFlush(text);
        this.sendTextMessage(text, channel.attr(CHANNEL_UNIQUE_KEY).get());
    }

    @Override
    public void sendTextMessage(String text, String channelKey) {
        queueMessageMap.get(channelKey).onNext(text);
//        Flux.just(text).map(Object::toString)
//                .subscribe(s->
//                        queueMessageMap.get(channelKey).onNext(s)
//                );
    }

    @Override
    public Flux<String> receiveTextMessage(Channel channel) {
        return queueMessageMap.get(channel.attr(CHANNEL_UNIQUE_KEY).get());
    }

    @Override
    public void onTextMessage(TextWebSocketFrame frame, Channel channel) {
        this.sendTextMessage(frame.text(), channel);
    }

    @Override
    public void onBinaryMessage(BinaryWebSocketFrame frame, Channel channel) {
        // channel.writeAndFlush(Unpooled.buffer(0));
    }

    @Override
    public void onPingMessage(PingWebSocketFrame frame, Channel channel) {
        channel.writeAndFlush(new PongWebSocketFrame());
    }

    @Override
    public void onPongMessage(PongWebSocketFrame frame, Channel channel) {
        channel.writeAndFlush(new PingWebSocketFrame());
    }

    @Override
    public void disconnect(Channel channel) {
        if (channel!=null && channel.attr(CHANNEL_UNIQUE_KEY) != null) {
            String channelKey = channel.attr(CHANNEL_UNIQUE_KEY).get();
            // queueMessageMap.get(channelKey).dispose();
            channelMap.remove(channelKey);
            queueMessageMap.remove(channelKey);
        }
        if (channel!=null && channel.isActive()) {
            channel.disconnect();
            channel.close();
        }
        // log.info("User leave : {}", channelMap.size());
    }

    public Map<String, Channel> getChannelMap() {
        return channelMap;
    }
}
