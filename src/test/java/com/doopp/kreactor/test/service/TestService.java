package com.doopp.kreactor.test.service;

import reactor.core.publisher.Mono;

public interface TestService {

    Mono<String> serviceTest();
}
