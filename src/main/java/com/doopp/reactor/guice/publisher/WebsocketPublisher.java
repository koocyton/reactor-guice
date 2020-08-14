package com.doopp.reactor.guice.publisher;

import com.doopp.reactor.guice.RequestAttribute;
import com.doopp.reactor.guice.websocket.WebSocketServerHandle;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.UnicastProcessor;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import reactor.netty.http.server.WebsocketServerSpec;

import java.util.regex.Pattern;

public class WebsocketPublisher {

    private static final Logger log = LoggerFactory.getLogger(WebsocketPublisher.class);


    private static final Pattern PATTERN = Pattern.compile("\\s*,\\s*");

    // the out put
    private static Flux<Object> rp = UnicastProcessor.create();

    public Mono<Object> sendMessage2(HttpServerRequest request,
                                    HttpServerResponse response,
                                    WebSocketServerHandle handleObject,
                                    Object requestAttributeObject) {

        return Mono.just((RequestAttribute) requestAttributeObject)
                .flatMap(requestAttribute ->
                        response.header("content-type", "text/plain")
                                .sendWebsocket((in, out) -> {
                                    // return
                                    return out.withConnection(
                                            connect -> {
                                                // channel
                                                Channel channel = connect.channel();
                                                // System.out.println("\n\n >>>" + in);
                                                // System.out.println(channel);
                                                // on disconnect
                                                connect.onDispose().subscribe(null, null, () -> {
                                                    // System.out.println(handleObject);
                                                    handleObject.onClose(null, channel);
                                                });
                                                // set requestAttribute to channel
                                                channel.attr(RequestAttribute.REQUEST_ATTRIBUTE).set(requestAttribute);
                                                // set channel to requestAttribute
                                                // requestAttribute.setAttribute(CURRENT_CHANNEL, channel);
                                                // on connect
                                                handleObject.onConnect(channel);
                                                // System.out.println("on connect");
                                                // receive frame
                                                in.aggregateFrames().receiveFrames().subscribe(
                                                        frame->handleObject.handleEvent(frame, channel)
                                                );
                                            })
                                            // options
                                            // .options(NettyPipeline.SendOptions::flushOnEach)
                                            // .sendObject(Flux.just("abc".getBytes()));
                                            .sendObject(rp);
                                })
                );
    }

    public Mono<Object> sendMessage(HttpServerRequest request, HttpServerResponse response, WebSocketServerHandle handleObject, Object requestAttributeObject) {
        WebsocketServerSpec spec = WebsocketServerSpec.builder().build();
        String secWebSocketProtocol = handleObject.secWebSocketProtocol(request);
        if (secWebSocketProtocol!=null && !secWebSocketProtocol.equals("")) {
            String[] requestedSubprotocolArray = PATTERN.split(secWebSocketProtocol);
            secWebSocketProtocol = requestedSubprotocolArray[0];
            spec = WebsocketServerSpec.builder().protocols(secWebSocketProtocol).build();
        }
        // return
        return response.addHeader("content-type", "text/plain")
                .sendWebsocket((in, out) -> {
                    // return
                    return out.withConnection(
                            connect -> {
                                Channel channel = connect.channel();
                                connect.onDispose().subscribe(null, null, () -> {
                                    handleObject.onClose(null, channel);
                                });
                                channel.attr(RequestAttribute.REQUEST_ATTRIBUTE).set((RequestAttribute) requestAttributeObject);
                                handleObject.onConnect(channel);
                                in.aggregateFrames().receiveFrames().subscribe(
                                        frame->handleObject.handleEvent(frame, channel)
                                );
                            })
                            .sendString(UnicastProcessor.create());
                }, spec)
                .then(Mono.empty());
    }

    public Mono<Object> sendMessage5(HttpServerRequest request,
                                     HttpServerResponse response,
                                     WebSocketServerHandle handleObject,
                                     Object requestAttributeObject) {
        return response.header("content-type", "text/plain")
                .sendWebsocket((in, out) ->
                        out.sendString(in.receive()
                                .asString()
                                .publishOn(Schedulers.single())
                                // .doOnNext(s -> serverRes.incrementAndGet())
                                // .map(it -> it + ' ' + request.param("param") + '!')
                                .map(s-> "a")))
                .then(Mono.empty());
    }
}
