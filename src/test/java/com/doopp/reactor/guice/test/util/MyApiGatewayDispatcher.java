package com.doopp.reactor.guice.test.util;

import com.doopp.reactor.guice.ApiGatewayDispatcher;

import java.net.MalformedURLException;
import java.net.URL;

public class MyApiGatewayDispatcher implements ApiGatewayDispatcher {

    @Override
    public URL getInsideUrl(String uri) {
        try {
            return new URL("https://www.baidu.com" + uri);
        }
        catch(MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
