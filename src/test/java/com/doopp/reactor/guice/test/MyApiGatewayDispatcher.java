package com.doopp.reactor.guice.test;

import com.doopp.reactor.guice.ApiGatewayDispatcher;

public class MyApiGatewayDispatcher implements ApiGatewayDispatcher {

    @Override
    public String getInsideUrl(String uri) {
        return "https://www.doopp.com" + uri;
    }
}
