package com.doopp.reactor.guice.test.service;

import reactor.core.publisher.Mono;

public interface TestService {

    Mono<String> serviceTest();

    void filterCallTest();
}
