package com.doopp.reactor.guice.test.service.impl;

import com.doopp.reactor.guice.annotation.Service;
import com.doopp.reactor.guice.test.service.TestService;
import com.google.inject.Inject;
import io.netty.util.CharsetUtil;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

@Service
public class TestServiceImpl implements TestService {

    @Inject
    private HttpClient httpClient;

    @Override
    public Mono<String> serviceTest() {
        return httpClient
            .get()
            .uri("https://www.doopp.com")
            .responseContent()
            .aggregate()
            .map(byteBuf -> byteBuf.toString(CharsetUtil.UTF_8));
    }

    @Override
    public void filterCallTest() {
        // System.out.println("filterCallTest");
    }
}
