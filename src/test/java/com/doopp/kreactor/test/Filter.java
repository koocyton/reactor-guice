package com.doopp.kreactor.test;

import com.doopp.kreactor.common.KReactorFilter;
import com.doopp.kreactor.common.RequestAttribute;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

class Filter implements KReactorFilter {

    @Override
    public Mono<Object> doFilter(HttpServerRequest request, HttpServerResponse response, RequestAttribute requestAttribute) {
        return Mono.just(requestAttribute);
        // return Mono.error(new KReactorException(100, "zzz"));
    }
}
