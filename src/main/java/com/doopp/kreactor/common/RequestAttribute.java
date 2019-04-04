package com.doopp.kreactor.common;

import io.netty.util.AttributeKey;

import java.util.HashMap;
import java.util.Map;

public class RequestAttribute {

    public static AttributeKey<RequestAttribute> REQUEST_ATTRIBUTE = AttributeKey.newInstance("request_attribute");

    private Map<String, Object> attributes = new HashMap<>();

    public <T> void setAttribute(String key, T object) {
        this.attributes.put(key, object);
    }

    public <T> T getAttribute(String key, Class<T> clazz) {
        return clazz.cast(this.attributes.get(key));
    }
}
