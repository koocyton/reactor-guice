package com.doopp.kreactor.test.service.impl;

import com.doopp.kreactor.test.service.TestService;
import com.google.inject.Inject;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.nio.charset.Charset;

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
            .map(byteBuf -> byteBuf.toString(Charset.forName("UTF-8")));
    }

    @Override
    public void filterCallTest() {
        // System.out.println("filterCallTest");
    }
}
