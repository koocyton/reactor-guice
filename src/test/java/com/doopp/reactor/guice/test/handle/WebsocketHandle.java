package com.doopp.reactor.guice.test.handle;

import com.doopp.reactor.guice.websocket.AbstractWebSocketServerHandle;
import com.google.inject.Singleton;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import reactor.netty.http.server.HttpServerRequest;

import javax.ws.rs.Path;

@Path("/kreactor/ws")
@Singleton
public class WebsocketHandle extends AbstractWebSocketServerHandle {

    @Override
    public String secWebSocketProtocol(HttpServerRequest request) {
        return "User-Token";
    }

    @Override
    public void onTextMessage(TextWebSocketFrame frame, Channel channel) {
        this.sendTextMessage(frame.text(), channel);
    }
}
