package com.doopp.reactor.guice.test;

import com.doopp.reactor.guice.ReactorGuiceServer;
import com.doopp.reactor.guice.test.proto.hello.Hello;
import com.doopp.reactor.guice.test.util.MyGsonHttpMessageConverter;
import com.doopp.reactor.guice.view.FreemarkTemplateDelegate;
import com.google.inject.name.Names;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.CharsetUtil;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxProcessor;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ReplayProcessor;
import reactor.netty.NettyPipeline;
import reactor.netty.http.client.HttpClient;

import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.Properties;

public class AppServer {

    @Test
    public void testServer() throws IOException, InterruptedException {

        Properties properties = new Properties();
        // properties.load(new FileInputStream("D:\\project\\reactor-guice\\application.properties"));
        properties.load(new FileInputStream("/Developer/Project/reactor-guice/application.properties"));



        String host = properties.getProperty("server.host", "127.0.0.1");
        int port = Integer.valueOf(properties.getProperty("server.port", "8083"));
        int sslPort = Integer.valueOf(properties.getProperty("server.sslPort", "8084"));

        String jksFile = properties.getProperty("server.jks.file", "127.0.0.1");
        String jksPassword = properties.getProperty("server.jks.password", "");
        String jksSecret = properties.getProperty("server.jks.secret", "");

        System.out.println(">>> http://" + host + ":" + port + "/");
        System.out.println(">>> http://" + host + ":" + port + "/kreactor/test/json");
        System.out.println(">>> http://" + host + ":" + port + "/kreactor/test/html/123");
        System.out.println(">>> http://" + host + ":" + port + "/kreactor/test/image");
        System.out.println(">>> http://" + host + ":" + port + "/kreactor/test/points");
        System.out.println(">>> http://" + host + ":" + port + "/kreactor/test/redirect");
        System.out.println(">>> http://" + host + ":" + port + "/kreactor/test/params\n");

        String jksFilePath = null;//getClass().getResource("/"+jksFile).getPath();

        ReactorGuiceServer.create()
            .bind(host, port, sslPort)
            .createInjector(
                binder -> Names.bindProperties(binder, properties),
                new Module()
            )
            // .setHttpMessageConverter(new MyJacksonHttpMessageConverter())
            .setHttpMessageConverter(new MyGsonHttpMessageConverter())
            .setTemplateDelegate(new FreemarkTemplateDelegate())
            // .setTemplateDelegate(new ThymeleafTemplateDelegate())
            .basePackages("com.doopp.reactor.guice.test")
            .addFilter("/", TestFilter.class)
            .addResource("/static/", "/static-public/")
            .addResource("/", "/public/")
            // .setHttps(new File(jksFilePath), jksPassword, jksSecret)
            // .setTestHttps()
            .printError(true)
            // .crossOrigin(true)
            .launch();
    }

    @Test
    public void testProtobufClient() {

        byte[] bt = HttpClient.create().get()
            .uri("http://127.0.0.1:8083/kreactor/test/protobuf")
            .responseContent()
            .aggregate()
            .map(byteBuf -> {
                byte[] abc = new byte[byteBuf.readableBytes()];
                byteBuf.readBytes(abc);
                return abc;
            })
            .block();

        try {
            System.out.println(Hello.parseFrom(bt));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Test
    public void testPostProtobufBean() {

        Hello.Builder builder = Hello.newBuilder();
        builder.setId(123);
        builder.setName("wuyi");
        builder.setEmail("wuyi@doopp.com");

        ByteBuf buf = Unpooled.wrappedBuffer(builder.build().toByteArray()).retain();

        for(int ii=0; ii<100000; ii++) {
            String hhe = HttpClient.create()
                .headers(headers -> {
                    headers.add(HttpHeaderNames.CONTENT_TYPE, "application/x-protobuf");
                })
                .post()
                .uri("http://127.0.0.1:8083/kreactor/test/proto-post-bean")
                .send(Flux.just(buf.retain()))
                .responseSingle((res, content) -> content)
                .map(byteBuf -> byteBuf.toString(CharsetUtil.UTF_8))
                .block();

            System.out.println("" + ii + " : " + hhe);
        }
        System.out.println("ok");
    }

    @Test
    public void testPostJsonBean() {

        for(int ii=0; ii<100000; ii++) {
            ByteBuf buf = Unpooled.wrappedBuffer("{\"id\":\"123123121312312\", \"name\":\"wuyi\"}".getBytes()).retain();

            String hhe = HttpClient.create()
                .headers(headers -> {
                    headers.add(HttpHeaderNames.CONTENT_TYPE, MediaType.APPLICATION_JSON);
                })
                .post()
                .uri("http://127.0.0.1:8083/kreactor/test/post-bean")
                .send(Flux.just(buf))
                .responseSingle((res, content) -> content)
                .map(byteBuf -> byteBuf.toString(CharsetUtil.UTF_8))
                .block();

            System.out.println("" + ii + " : " + hhe);
        }
    }

    @Test
    public void testPostFormBean() {
        for (int ii=0; ii<100000; ii++) {
            String hhe = HttpClient.create()
                .post()
                .uri("http://127.0.0.1:8083/kreactor/test/post-bean")
                .sendForm((req, form) -> form.multipart(false)
                    .attr("id", "123123121312312")
                    .attr("account", "account")
                    .attr("password", "password")
                    .attr("name", "name")
                )
                .responseSingle((res, content) -> content)
                .map(byteBuf -> byteBuf.toString(CharsetUtil.UTF_8))
                .block();
            System.out.println("" + ii + " : " + hhe);
        }
    }

    @Test
    public void testFileUpload() {
        for(int ii=0; ii<10000; ii++) {
            String hhe = HttpClient.create()
                .post()
                // .uri("http://0.0.0.0:8085/api/login")
                .uri("http://127.0.0.1:8083/kreactor/test/post-bean")
                .sendForm((req, form) -> form.multipart(true)
                    .attr("id", "123123121312312")
                    .attr("account", "liuyi")
                    .attr("password", "password")
                    .attr("name", "name")
                    .file("image", new File("/Users/henry/Pictures/girl.jpg"))
                    // .file("image", new File("C:\\Users\\koocyton\\Pictures\\cloud.jpg"))
                    // .file("image", new File("C:\\Users\\koocyton\\Pictures\\st.jpg"))
                    // .file("image", new File("C:\\Users\\koocyton\\Pictures\\zz.txt"))
                )
                .responseSingle((res, content) -> content)
                .map(byteBuf -> byteBuf.toString(CharsetUtil.UTF_8))
                .block();

            System.out.println("" + ii + " : " + hhe);
        }
    }

    @Test
    public void testWebsocketClient1() throws IOException {
        testWebsocketClient();
    }

    @Test
    public void testWebsocketClient2() throws IOException {
        testWebsocketClient();
    }

    @Test
    public void testWebsocketClient3() throws IOException {
        testWebsocketClient();
    }

    @Test
    public void testWebsocketClient4() throws IOException {
        testWebsocketClient();
    }

    private static void testWebsocketClient() throws IOException {
        Properties properties = new Properties();
        // properties.load(new FileInputStream("D:\\project\\reactor-guice\\application.properties"));
        properties.load(new FileInputStream("/Developer/Project/reactor-guice/application.properties"));

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
                out.withConnection(conn -> {
                    in.aggregateFrames().receiveFrames().map(frames -> {
                        if (frames instanceof TextWebSocketFrame) {
                            System.out.println("Receive text message " + ((TextWebSocketFrame) frames).text());
                        }
                        else if (frames instanceof BinaryWebSocketFrame) {
                            System.out.println("Receive binary message " + frames.content());
                        }
                        else {
                            System.out.println("Receive normal message " + frames.content());
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
}
