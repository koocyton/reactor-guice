package com.doopp.reactor.guice.test.util;

import com.doopp.reactor.guice.ApiGatewayDispatcher;

public class MyApiGatewayDispatcher implements ApiGatewayDispatcher {

    @Override
    public String insideUrl(String uri) {
        if (uri.equals("/kreactor-rr/ws")) {
            return "ws://127.0.0.1:8083/kreactor/ws";
        }
        return "https://www.baidu.com" + uri;
    }
}
