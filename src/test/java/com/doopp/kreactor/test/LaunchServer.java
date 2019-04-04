package com.doopp.kreactor.test;

import com.doopp.kreactor.KReactorServer;
import com.doopp.kreactor.test.handle.TestHandle;
import com.google.inject.*;
import com.google.inject.name.Names;
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

public class LaunchServer {

    @Test
    public void test() throws IOException {

        Properties properties = testProperties();

        Injector injector = Guice.createInjector(
            binder -> Names.bindProperties(binder, properties),
            new Module()
        );

        String host = properties.getProperty("server.host", "127.0.0.1");
        int port = Integer.valueOf(properties.getProperty("server.port", "8081"));

        System.out.println(">>> http://"+host+":"+port+"/");
        System.out.println(">>> http://"+host+":"+port+"/kreactor/test/json");
        System.out.println(">>> http://"+host+":"+port+"/kreactor/test/html/123");
        System.out.println(">>> http://"+host+":"+port+"/kreactor/test/image\n");

        KReactorServer.create()
            .bind(host, port)
            .injector(injector)
            .basePackages(TestHandle.class.getPackage().getName())
            .addFilter("/", new Filter())
            .launch();
    }

    @Test
    public void testWebsocketClient() throws IOException {

        Properties properties = testProperties();
        int port = Integer.valueOf(properties.getProperty("server.port", "8081"));

        FluxProcessor<String, String> client = ReplayProcessor.<String>create().serialize();

        Flux.interval(Duration.ofMillis(1000))
            .map(Object::toString)
            .subscribe(client::onNext);

        HttpClient.create()
            .port(port)
            .wiretap(true)
            .websocket()
            .uri("/kreactor/ws")
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
        // properties.load(new FileInputStream("D:\\project\\reactor-guice\\reactor-guice\\application.properties"));
        properties.load(new FileInputStream("/Developer/Project/reactor-guice/application.properties"));
        return properties;
    }
}


/*

mvn clean

mvn package

mvn install:install-file -Dfile=./target/kreactor-1.0-SNAPSHOT.jar -DgroupId=com.doopp -DartifactId=reactor-guice -Dversion=0.0.3 -Dpackaging=jar

 */