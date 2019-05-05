package com.doopp.reactor.guice.publisher;

import com.doopp.reactor.guice.ApiGatewayDispatcher;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

public class ApiGatewayPublisher {

    private ApiGatewayDispatcher apiGatewayDispatcher;

    public ApiGatewayPublisher(ApiGatewayDispatcher apiGatewayDispatcher) {
        this.apiGatewayDispatcher = apiGatewayDispatcher;
    }

    public Mono<Object> sendResponse(HttpServerRequest req, HttpServerResponse resp) {
        return Mono.just("abc");
    }

    public Mono<Object> getResponse(String host, String uri) {
        return Mono.just("abc");
    }

    public Mono<Object> postResponse(String host, String uri, String postData) {
        return Mono.just("abc");
    }

    public Mono<Object> deleteResponse(String host, String uri) {
        return Mono.just("abc");
    }

    public Mono<Object> putResponse(String host, String uri, String postData) {
        return Mono.just("abc");
    }

    public boolean checkRequest(HttpServerRequest req) {
        return true;
    }
}
