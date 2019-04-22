package com.doopp.reactor.guice.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import java.io.IOException;

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
    public String toJson(Object object) {
        try {
            return this.objectMapper.writeValueAsString(object);
        }
        catch(JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> T fromJson(String json, Class<T> clazz) {
        try {
            return this.objectMapper.readValue(json, clazz);
        }
        catch(IOException e) {
            throw new RuntimeException(e);
        }
    }
}

