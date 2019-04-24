package com.doopp.reactor.guice.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.LongSerializationPolicy;

public class GsonHttpMessageConverter implements HttpMessageConverter {

    private Gson gson;

    public GsonHttpMessageConverter() {
        this.gson = new GsonBuilder()
            .serializeNulls()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .setLongSerializationPolicy(LongSerializationPolicy.STRING)
            .create();
    }

    public GsonHttpMessageConverter(Gson gson) {
        assert gson!=null : "A Gson instance is required";
        this.gson = gson;
    }

    public void setGson(Gson gson) {
        assert gson!=null : "A Gson instance is required";
        this.gson = gson;
    }

    public Gson getGson() {
        return this.gson;
    }

    @Override
    public String toJson(Object object) {
        return this.gson.toJson(object);
    }

    @Override
    public <T> T fromJson(String json, Class<T> clazz) {
        return this.gson.fromJson(json, clazz);
    }
}

