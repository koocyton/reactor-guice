package com.doopp.reactor.guice;

import io.netty.handler.codec.http.HttpResponseStatus;

public class StatusMessageException extends Exception {

    private int errorCode = 0;

    public StatusMessageException(int errorCode, String errorMessage) {
        super(errorMessage);
        this.errorCode = errorCode;
    }

    public StatusMessageException(HttpResponseStatus status, String errorMessage) {
        super(errorMessage);
        this.errorCode = status.code();
    }

    public StatusMessageException(HttpResponseStatus status) {
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
