package com.doopp.reactor.guice.test.handle;

import com.doopp.reactor.guice.websocket.AbstractWebSocketServerHandle;
import com.google.inject.Singleton;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.ws.rs.Path;
import java.time.Duration;

@Path("/kreactor/ws")
@Singleton
public class WebsocketHandle extends AbstractWebSocketServerHandle {

    @Override
    public Mono<Void> onTextMessage(TextWebSocketFrame frame, Channel channel) {
        for(int ii=0; ii<10; ii++) {
            this.sendTextMessage(frame.text(), channel);
        }
        return Mono.empty();
    }

    @Override
    public Mono<Void> onConnect(Channel channel) {
        // Flux.interval(Duration.ofMillis(1000))
        return super.onConnect(channel);
    }
}
