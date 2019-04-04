package com.doopp.kreactor.common;


import io.netty.handler.codec.http.HttpResponseStatus;

public class KReactorException extends Exception {

    private int errorCode = 0;

    public KReactorException(int errorCode, String errorMessage) {
        super(errorMessage);
        this.errorCode = errorCode;
    }

    public KReactorException(HttpResponseStatus status, String errorMessage) {
        super(errorMessage);
        this.errorCode = status.code();
    }

    public KReactorException(HttpResponseStatus status) {
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
