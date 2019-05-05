package com.doopp.reactor.guice.publisher;

import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

public class ApiGatewayPublisher {

    public Mono<Object> sendResponse(HttpServerRequest req, HttpServerResponse resp) {
        return Mono.just("abc");
    }

    public boolean checkRequest(HttpServerRequest req) {
        return true;
    }
}
