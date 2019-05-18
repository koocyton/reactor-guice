package com.doopp.reactor.guice.test;

import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxProcessor;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ReplayProcessor;
import reactor.netty.NettyPipeline;
import reactor.netty.http.client.HttpClient;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.Properties;

public class WebsocketClientTest {


    @Test
    public void testWebsocketClient() throws IOException {

        Properties properties = testProperties();
        int port = Integer.valueOf(properties.getProperty("server.port", "8081"));

        FluxProcessor<String, String> client = ReplayProcessor.<String>create().serialize();

        Flux.interval(Duration.ofMillis(1000))
            .map(Object::toString)
            .subscribe(client::onNext);

        HttpClient.create()
            // .port(port)
            // .wiretap(true)
            .websocket()
            .uri("ws://127.0.0.1:8083/kreactor/ws")
            .handle((in, out) ->
                out.withConnection(conn->{
                    in.aggregateFrames().receiveFrames().map(frames->{
                        if (frames instanceof TextWebSocketFrame) {
                            System.out.println(((TextWebSocketFrame) frames).text());
                        }
                        return Mono.empty();
                    })
                        .subscribe();
                })
                    .options(NettyPipeline.SendOptions::flushOnEach)
                    .sendString(client)
            )
            .blockLast();
    }

    private Properties testProperties() throws IOException {
        Properties properties = new Properties();
        // properties.load(new FileInputStream("D:\\project\\reactor-guice\\application.properties"));
        properties.load(new FileInputStream("/Developer/Project/reactor-guice/application.properties"));
        return properties;
    }
}
