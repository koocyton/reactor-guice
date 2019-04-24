package com.doopp.reactor.guice.json;

public interface HttpMessageConverter {

    String toJson(Object object);

    <T> T fromJson(String json, Class<T> clazz);
}

