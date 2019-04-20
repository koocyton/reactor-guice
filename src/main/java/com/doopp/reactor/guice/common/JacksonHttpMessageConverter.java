package com.doopp.reactor.guice.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.*;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.text.SimpleDateFormat;

public class JacksonHttpMessageConverter implements HttpMessageConverter {

    private ObjectMapper objectMapper;

    public JacksonHttpMessageConverter() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }

    public JacksonHttpMessageConverter(ObjectMapper objectMapper) {
        assert objectMapper!=null : "A ObjectMapper instance is required";
        this.objectMapper = objectMapper;
    }

    public void setObjectMapper(ObjectMapper objectMapper) {
        assert objectMapper!=null : "A ObjectMapper instance is required";
        this.objectMapper = objectMapper;
    }

    public ObjectMapper getObjectMapper() {
        return this.objectMapper;
    }

    @Override
    public Mono<String> toJson(Object object) {
        return Mono.create(sink->{
            try {
                sink.success(this.objectMapper.writeValueAsString(object));
            }
            catch(JsonProcessingException e) {
                sink.error(e);
            }
        });
    }

    @Override
    public <T> Mono<T> fromJson(String json, Class<T> clazz) {
        return Mono.create(sink->{
            try {
                sink.success(this.objectMapper.readValue(json, clazz));
            }
            catch(IOException e) {
                sink.error(e);
            }
        });
    }
}

