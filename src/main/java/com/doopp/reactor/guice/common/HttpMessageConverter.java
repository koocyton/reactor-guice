package com.doopp.reactor.guice.common;

public interface HttpMessageConverter {

    String toJson(Object object);

    <T> T fromJson(String json, Class<T> clazz);
}

