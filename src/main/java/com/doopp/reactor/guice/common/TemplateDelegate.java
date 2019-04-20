package com.doopp.reactor.guice.common;

import reactor.core.publisher.Mono;

public interface TemplateDelegate {

    Mono<String> templateMono(Object handleObject, ModelMap modelMap, String templateName);
}
