package com.doopp.reactor.guice.test;

import com.doopp.reactor.guice.filter.ReactorGuiceFilter;
import com.doopp.reactor.guice.RequestAttribute;
import com.doopp.reactor.guice.test.service.TestService;
import com.google.inject.Inject;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

class Filter implements ReactorGuiceFilter {

    @Inject
    private TestService testService;

    @Override
    public Mono<Object> doFilter(HttpServerRequest request, HttpServerResponse response, RequestAttribute requestAttribute) {
        testService.filterCallTest();
        return Mono.just(requestAttribute);
        // return Mono.error(new KReactorException(100, "zzz"));
    }
}
