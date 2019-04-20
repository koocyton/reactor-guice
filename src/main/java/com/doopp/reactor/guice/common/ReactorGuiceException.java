package com.doopp.reactor.guice.common;

import io.netty.handler.codec.http.HttpResponseStatus;

public class ReactorGuiceException extends Exception {

    private int errorCode = 0;

    public ReactorGuiceException(int errorCode, String errorMessage) {
        super(errorMessage);
        this.errorCode = errorCode;
    }

    public ReactorGuiceException(HttpResponseStatus status, String errorMessage) {
        super(errorMessage);
        this.errorCode = status.code();
    }

    public ReactorGuiceException(HttpResponseStatus status) {
        super(status.reasonPhrase());
        this.errorCode = status.code();
    }

    public int getCode() {
        return this.errorCode;
    }

    public String getMessage() {
        return super.getMessage();
    }
}
