package com.doopp.reactor.guice.publisher;

import com.doopp.reactor.guice.ApiGatewayDispatcher;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.cookie.Cookie;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Set;

public class ApiGatewayPublisher {

    private HttpClient httpClient = HttpClient.create();

    private ApiGatewayDispatcher apiGatewayDispatcher;

    public ApiGatewayPublisher(ApiGatewayDispatcher apiGatewayDispatcher) {
        this.apiGatewayDispatcher = apiGatewayDispatcher;
    }

    public Mono<Object> sendResponse(HttpServerRequest req, HttpServerResponse resp) {

        String uri = req.uri();
        String host = this.apiGatewayDispatcher.getPrimaryAddress(uri);
        Map<CharSequence, Set<Cookie>> cookie = req.cookies();
        InetSocketAddress hostAddress = req.hostAddress();
        InetSocketAddress remoteAddress = req.remoteAddress();
        HttpHeaders requestHeaders = req.requestHeaders();

        if (req.method()== HttpMethod.POST) {
            return this.postResponse(host, uri, null);
        }
        else if (req.method()== HttpMethod.DELETE) {
            return this.deleteResponse(host, uri);
        }
        else if (req.method()== HttpMethod.PUT) {
            return this.putResponse(host, uri, null);
        }
        else {
            return this.getResponse(host, uri);
        }
    }

    private Mono<Object> getResponse(String host, String uri) {
        return httpClient.cookie();
    }

    private Mono<Object> postResponse(String host, String uri, String postData) {
        return Mono.just("abc");
    }

    private Mono<Object> deleteResponse(String host, String uri) {
        return Mono.just("abc");
    }

    private Mono<Object> putResponse(String host, String uri, String postData) {
        return Mono.just("abc");
    }

    public boolean checkRequest(HttpServerRequest req) {
        return true;
    }
}
