package com.doopp.kreactor.common;

import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

public interface KReactorFilter {

    Mono<Object> doFilter(HttpServerRequest request, HttpServerResponse response, RequestAttribute requestAttribute);

    default boolean isNeedFilter(String uri, String[] notNeedFilters) {
        boolean needFilter = true;
        for (String notNeedFilter : notNeedFilters) {
            if (uri.length() >= notNeedFilter.length() && uri.substring(0, notNeedFilter.length()).equals(notNeedFilter)) {
                needFilter = false;
                break;
            }
        }
        // 返回
        return needFilter;
    }
}
