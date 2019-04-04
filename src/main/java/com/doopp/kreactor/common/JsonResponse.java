package com.doopp.kreactor.common;

public class JsonResponse<T> {

    private int err_code = 0;

    private String err_msg = "";

    private T data;

    T getData() {
        return data;
    }

    void setData(T data) {
        this.data = data;
    }

    int getErr_code() {
        return err_code;
    }

    void setErr_code(int err_code) {
        this.err_code = err_code;
    }

    String getErr_msg() {
        return err_msg;
    }

    void setErr_msg(String err_msg) {
        this.err_msg = err_msg;
    }

    public JsonResponse(T data) {
        if (data instanceof KReactorException) {
            KReactorException _data = (KReactorException) data;
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
