package com.doopp.reactor.guice.test.util;

import com.doopp.reactor.guice.StatusMessageException;
import com.doopp.reactor.guice.json.HttpMessageConverter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.LongSerializationPolicy;
import com.google.gson.annotations.Expose;

public class MyGsonHttpMessageConverter implements HttpMessageConverter {

    private Gson gson;

    public MyGsonHttpMessageConverter() {
        this.gson = new GsonBuilder()
            .serializeNulls()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .setLongSerializationPolicy(LongSerializationPolicy.STRING)
            // .excludeFieldsWithoutExposeAnnotation()
            .create();
    }

    public MyGsonHttpMessageConverter(Gson gson) {
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
        return this.gson.toJson(new response(object));
    }

    @Override
    public <T> T fromJson(String json, Class<T> clazz) {
        return this.gson.fromJson(json, clazz);
    }

    private class response {

        @Expose
        private int err_code = 0;

        @Expose
        private String err_msg = "";

        @Expose
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

