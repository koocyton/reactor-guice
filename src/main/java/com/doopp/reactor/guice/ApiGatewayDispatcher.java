package com.doopp.reactor.guice;

import java.net.URL;

public interface ApiGatewayDispatcher {

    URL getInsideUrl(String uri);
}
