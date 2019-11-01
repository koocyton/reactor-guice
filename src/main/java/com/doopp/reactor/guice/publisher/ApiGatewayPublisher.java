package com.doopp.reactor.guice.publisher;

import com.doopp.reactor.guice.ApiGatewayDispatcher;
import com.doopp.reactor.guice.RequestAttribute;
import com.doopp.reactor.guice.websocket.AbstractWebSocketServerHandle;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.websocketx.*;
import reactor.core.publisher.FluxProcessor;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ReplayProcessor;
import reactor.netty.NettyPipeline;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ApiGatewayPublisher {

    private ApiGatewayDispatcher apiGatewayDispatcher;

    private GatewayWsHandle gatewayWsHandle;

    public ApiGatewayPublisher(ApiGatewayDispatcher apiGatewayDispatcher) {
        this.apiGatewayDispatcher = apiGatewayDispatcher;
        this.gatewayWsHandle = new GatewayWsHandle();
    }

    public Mono<Object> sendResponse(HttpServerRequest req, HttpServerResponse resp, WebsocketPublisher websocketPublisher, Object requestAttribute) {

        String insideUrlPath = this.apiGatewayDispatcher.insideUrl(req.uri());

        // System.out.println(req.uri() + "\n" + req.requestHeaders());

        // if (req.requestHeaders().get("upgrade")!=null && req.requestHeaders().get("upgrade").equals("websocket")) {
        if (req.isWebsocket()) {
            if (requestAttribute instanceof RequestAttribute) {
                ((RequestAttribute) requestAttribute).setAttribute("websocket-inside-url", insideUrlPath);
            }
            return websocketPublisher.sendMessage(req, resp, this.gatewayWsHandle, requestAttribute);
        }

        URL insideUrl;
        try {
            insideUrl = new URL(insideUrlPath);
        } catch (Exception e) {
            return Mono.error(e);
        }

        HttpClient httpClient = HttpClient.create()
            .headers(httpHeaders -> {
                // set headers
                HttpHeaders headers = req.requestHeaders();
                headers.forEach(action->{
                    if (!action.getKey().equals("Host")) {
                        httpHeaders.set(action.getKey(), action.getValue());
                    }
                });
                httpHeaders.set("Host", insideUrl.getHost());
                // set cookie
                req.cookies().forEach((charSequence, cookies)->{
                    StringBuilder cookieString = new StringBuilder();
                    for (Cookie cookie : cookies) {
                        if (!cookieString.toString().equals("")) {
                            cookieString.append("; ");
                        }
                        cookieString.append(cookie.name()).append("=").append(cookie.value());
                    }
                    httpHeaders.set("Cookie", cookieString.toString());
                });
            })
            .keepAlive(true)
            .tcpConfiguration(tcpClient ->
                tcpClient.option(ChannelOption.SO_KEEPALIVE, true)
            );

        if (req.method() == HttpMethod.POST || req.method() == HttpMethod.PUT) {
            HttpClient.RequestSender sender = (req.method() == HttpMethod.POST) ? httpClient.post() : httpClient.put();
            return req
                .receive()
                .aggregate()
                .flatMap(byteBuf ->
                    sender
                        .uri(insideUrl.toString())
                        .send(Mono.just(byteBuf.retain()))
                        .responseSingle((sResp, sMonoBf) -> {
                            resp.status(sResp.status()).headers(sResp.responseHeaders());
                            return sMonoBf;
                        })
                        .map(ByteBuf::retain)
                );
        }
        else if (req.method() == HttpMethod.DELETE) {
            return httpClient
                    .delete()
                    .uri(insideUrl.toString())
                    .responseSingle((sResp, sMonoBf) -> {
                        resp.status(sResp.status()).headers(sResp.responseHeaders());
                        return sMonoBf;
                    })
                    .map(ByteBuf::retain);
        }
        else {
            return httpClient
                    .get()
                    .uri(insideUrl.toString())
                    .responseSingle((sResp, sMonoBf) -> {
                        resp.status(sResp.status()).headers(sResp.responseHeaders());
                        return sMonoBf;
                    })
                    .map(ByteBuf::retain);
        }
    }

    public boolean checkRequest(HttpServerRequest req) {
        return true;
    }

    private class GatewayWsHandle extends AbstractWebSocketServerHandle {

        private Map<String, HttpClient.WebsocketSender> clients = new ConcurrentHashMap<>();

        private Map<String, FluxProcessor<WebSocketFrame, WebSocketFrame>> messages = new ConcurrentHashMap<>();

        @Override
        public Mono<Void> onConnect(Channel channel) {

            RequestAttribute requestAttribute = channel.attr(RequestAttribute.REQUEST_ATTRIBUTE).get();
            String wsUrl = requestAttribute.getAttribute("websocket-inside-url", String.class);

            if (wsUrl==null) {
                return this.onClose(null, channel);
            }

            String channelId = channel.id().asLongText();

            messages.put(channelId, ReplayProcessor.<WebSocketFrame>create().serialize());

            clients.put(channelId, HttpClient
                .create()
                .websocket()
                .uri(wsUrl));

            return clients.get(channelId).handle((in, out) -> out
                .withConnection(con->{
                    // channel
                    Channel ch = con.channel();
                    // on disconnect
                    con.onDispose().subscribe(null, null, () ->{
                        if (ch.isOpen() && ch.isActive()) {
                            ch.close();
                            clients.remove(ch.id().asLongText());
                            messages.remove(ch.id().asLongText());
                        }
                    });
                    in.aggregateFrames().receiveFrames().subscribe(frame -> {
                        if (frame instanceof CloseWebSocketFrame && ch.isOpen() && ch.isActive()) {
                            ch.close();
                            clients.remove(ch.id().asLongText());
                            messages.remove(ch.id().asLongText());
                            return;
                        }
                        channel.writeAndFlush(frame.retain());
                    });
                })
                .options(NettyPipeline.SendOptions::flushOnEach)
                .sendObject(messages.get(channelId))
            ).then();
        }

        @Override
        public Mono<Void> handleEvent(WebSocketFrame frame, Channel channel) {
            System.out.println(frame);
            String channelId = channel.id().asLongText();
            messages.get(channelId).onNext(frame.retain());
            return Mono.empty();
        }
    }
}
