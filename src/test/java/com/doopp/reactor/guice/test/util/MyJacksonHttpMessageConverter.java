package com.doopp.reactor.guice.test.util;

import com.doopp.reactor.guice.StatusMessageException;
import com.doopp.reactor.guice.json.HttpMessageConverter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;

public class MyJacksonHttpMessageConverter implements HttpMessageConverter {

    private ObjectMapper objectMapper;

    public MyJacksonHttpMessageConverter() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }

    public MyJacksonHttpMessageConverter(ObjectMapper objectMapper) {
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
            return this.objectMapper.writeValueAsString(new response(object));
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

    private class response {

        private int err_code = 0;

        private String err_msg = "";

        private Object data;

        public Object getData() {
            return data;
        }

        public void setData(Object data) {
            this.data = data;
        }

        public int getErr_code() {
            return err_code;
        }

        public void setErr_code(int err_code) {
            this.err_code = err_code;
        }

        public String getErr_msg() {
            return err_msg;
        }

        public void setErr_msg(String err_msg) {
            this.err_msg = err_msg;
        }

        public response(Object data) {
            if (data instanceof StatusMessageException) {
                StatusMessageException _data = (StatusMessageException) data;
                this.setErr_code(_data.getCode());
                this.setErr_msg(_data.getMessage());
            }
            else if (data instanceof Exception) {
                Exception _data = (Exception) data;
                this.setErr_code(500);
                this.setErr_msg(_data.getMessage());
            }
            else {
                this.data = data;
            }
        }

        public String toString()
        {
            return "{" +
                "\"err_code\":"+this.err_code+", " +
                "\"err_msg\":\""+this.err_msg+"\"" +
                "\"data\":\""+this.data.toString()+"\"" +
                "}";
        }
    }
}

