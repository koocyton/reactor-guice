package com.doopp.reactor.guice;

import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

public interface Filter {

    Mono<Object> doFilter(HttpServerRequest request, HttpServerResponse response, RequestAttribute requestAttribute);

    default boolean isNeedFilter(String uri, String[] notNeedFilters) {
        boolean needFilter = true;
        for (String notNeedFilter : notNeedFilters) {
            if (uri.length() >= notNeedFilter.length() && uri.startsWith(notNeedFilter)) {
                needFilter = false;
                break;
            }
        }
        // 返回
        return needFilter;
    }
}
