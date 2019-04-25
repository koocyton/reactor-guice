package com.doopp.reactor.guice;

public class StatusMessageResponse {

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

    public StatusMessageResponse(Object data) {
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
