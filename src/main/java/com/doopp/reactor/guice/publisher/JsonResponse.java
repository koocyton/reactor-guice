package com.doopp.reactor.guice.publisher;

import com.doopp.reactor.guice.common.ReactorGuiceException;

public class JsonResponse<T> {

    private int err_code = 0;

    private String err_msg = "";

    private T data;

    public T getData() {
        return data;
    }

    public void setData(T data) {
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

    public JsonResponse(T data) {
        if (data instanceof ReactorGuiceException) {
            ReactorGuiceException _data = (ReactorGuiceException) data;
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
}
