package com.doopp.reactor.guice.test;

import com.doopp.reactor.guice.ReactorGuiceServer;
import com.doopp.reactor.guice.test.proto.hello.Hello;
import com.doopp.reactor.guice.test.util.MyApiGatewayDispatcher;
import com.doopp.reactor.guice.test.util.MyGsonHttpMessageConverter;
import com.doopp.reactor.guice.view.FreemarkTemplateDelegate;
import com.google.inject.*;
import com.google.inject.name.Names;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpHeaderNames;
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

public class AppServerTest {

    @Test
    public void testServer() throws IOException {

        Properties properties = new Properties();
        properties.load(new FileInputStream("D:\\project\\reactor-guice\\application.properties"));
        // properties.load(new FileInputStream("/Developer/Project/reactor-guice/application.properties"));

        Injector injector = Guice.createInjector(
                binder -> Names.bindProperties(binder, properties),
                new Module()
        );

        String host = properties.getProperty("server.host", "127.0.0.1");
        int port = Integer.valueOf(properties.getProperty("server.port", "8081"));

        System.out.println(">>> http://"+host+":"+port+"/");
        System.out.println(">>> http://"+host+":"+port+"/kreactor/test/json");
        System.out.println(">>> http://"+host+":"+port+"/kreactor/test/html/123");
        System.out.println(">>> http://"+host+":"+port+"/kreactor/test/image");
        System.out.println(">>> http://"+host+":"+port+"/kreactor/test/points");
        System.out.println(">>> http://"+host+":"+port+"/kreactor/test/redirect");
        System.out.println(">>> http://"+host+":"+port+"/kreactor/test/params\n");


        ReactorGuiceServer.create()
            .bind(host, port)
            .injector(injector)
            // .setHttpMessageConverter(new MyJacksonHttpMessageConverter())
            .setHttpMessageConverter(new MyGsonHttpMessageConverter())
            .setTemplateDelegate(new FreemarkTemplateDelegate())
            // .setTemplateDelegate(new ThymeleafTemplateDelegate())
            .handlePackages("com.doopp.reactor.guice.test.handle")
            .addFilter("/", TestFilter.class)
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
        }
        catch(Exception e) {
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

        String hhe = HttpClient.create()
            .headers(headers->{
                headers.add(HttpHeaderNames.CONTENT_TYPE, "application/x-protobuf");
            })
            .post()
            .uri("http://127.0.0.1:8083/kreactor/test/proto-post-bean")
            .send(Flux.just(buf))
            .responseSingle((res, content) -> content)
            .map(byteBuf -> byteBuf.toString(CharsetUtil.UTF_8))
            .block();

        System.out.println(hhe);
    }

    @Test
    public void testPostJsonBean() {

        ByteBuf buf = Unpooled.wrappedBuffer("{\"id\":\"123123121312312\", \"name\":\"wuyi\"}".getBytes()).retain();

        String hhe = HttpClient.create()
                .headers(headers->{
                    headers.add(HttpHeaderNames.CONTENT_TYPE, MediaType.APPLICATION_JSON);
                })
                .post()
                .uri("http://127.0.0.1:8083/kreactor/test/post-bean")
                .send(Flux.just(buf))
                .responseSingle((res, content) -> content)
                .map(byteBuf -> byteBuf.toString(CharsetUtil.UTF_8))
                .block();

        System.out.println(hhe);
    }

    @Test
    public void testPostFormBean() {
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
        System.out.println(hhe);
    }

    @Test
    public void testFileUpload() {

        String hhe = HttpClient.create()
            .post()
            .uri("http://127.0.0.1:8083/kreactor/test/post-bean")
            .sendForm((req, form) -> form.multipart(true)
                .attr("id", "123123121312312")
                .attr("account", "account")
                .attr("password", "password")
                .attr("name", "name")
                .file("image", new File("C:\\Users\\koocyton\\Pictures\\cloud.jpg"))
            )
            .responseSingle((res, content) -> content)
            .map(byteBuf -> byteBuf.toString(CharsetUtil.UTF_8))
            .block();

        System.out.println(hhe);
    }
}
