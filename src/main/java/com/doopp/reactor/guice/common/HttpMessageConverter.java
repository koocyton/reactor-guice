package com.doopp.reactor.guice.common;

import reactor.core.publisher.Mono;

public interface HttpMessageConverter {

    Mono<String> toJson(Object object);

    <T> Mono<T> fromJson(String json, Class<T> clazz);
}

