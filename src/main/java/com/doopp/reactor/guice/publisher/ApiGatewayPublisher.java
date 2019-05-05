package com.doopp.reactor.guice.publisher;

import com.doopp.reactor.guice.ApiGatewayDispatcher;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelOption;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.util.CharsetUtil;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

public class ApiGatewayPublisher {

    private ApiGatewayDispatcher apiGatewayDispatcher;

    public ApiGatewayPublisher(ApiGatewayDispatcher apiGatewayDispatcher) {
        this.apiGatewayDispatcher = apiGatewayDispatcher;
    }

    public Mono<Object> sendResponse(HttpServerRequest req, HttpServerResponse resp) {

        String uri = req.uri();
        // String baseUrl = this.apiGatewayDispatcher.getPrimaryAddress(uri);
        String baseUrl = "https://www.doopp.com/config.json";

        HttpClient httpClient = HttpClient.create()
            .headers(httpHeaders -> {
                // set headers
                httpHeaders.set(req.requestHeaders());
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
                tcpClient.port(1080)
                    .option(ChannelOption.SO_KEEPALIVE, true)
            );



        if (req.method()== HttpMethod.POST) {
            return req
                .receive()
                .aggregate()
                .flatMap(byteBuf ->
                    httpClient
                        .post()
                        .uri(baseUrl)
                        .send(Flux.just(byteBuf))
                        .responseSingle((res, content) ->
                            content.retain()
                        )
                );
        }




        else if (req.method()== HttpMethod.DELETE) {
            return httpClient
                .delete()
                .uri(baseUrl)
                .responseContent()
                .aggregate()
                .map(ByteBuf::retain);
        }




        else if (req.method()== HttpMethod.PUT) {
            return req
                .receive()
                .aggregate()
                .flatMap(byteBuf ->
                    httpClient
                        .put()
                        .uri(baseUrl)
                        .send(Flux.just(byteBuf))
                        .responseSingle((res, content) ->
                            content.retain()
                        )
                );
        }




        else {
            return httpClient.get()
                .uri("https://www.doopp.com")
                .responseContent()
                .aggregate()
                // .map(ByteBuf::retain);
                .map(byteBuf -> {
                    String abc = byteBuf.toString(CharsetUtil.UTF_8);
                    System.out.println(abc);
                    return abc;
                });
        }
    }

    public boolean checkRequest(HttpServerRequest req) {
        return true;
    }
}
