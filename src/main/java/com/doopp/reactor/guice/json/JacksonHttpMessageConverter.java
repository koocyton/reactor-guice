package com.doopp.reactor.guice.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.io.IOException;
import java.text.SimpleDateFormat;

public class JacksonHttpMessageConverter implements HttpMessageConverter {

    private ObjectMapper objectMapper;

    public JacksonHttpMessageConverter() {
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(Long.class, ToStringSerializer.instance);
        simpleModule.addSerializer(Long.TYPE, ToStringSerializer.instance);

        this.objectMapper = (new ObjectMapper())
                // 解决实体未包含字段反序列化时抛出异常
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                // 对于空的对象转json的时候不抛出错误
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                // 允许属性名称没有引号
                .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
                // 允许单引号
                .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
                // 时间格式化
                .setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"))
                // 驼峰转蛇型
                .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
                // Long 转字符串
                .registerModule(simpleModule);
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

