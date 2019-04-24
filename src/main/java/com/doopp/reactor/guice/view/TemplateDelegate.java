package com.doopp.reactor.guice.view;

import freemarker.template.TemplateException;
import reactor.core.publisher.Mono;

import java.io.IOException;

public interface TemplateDelegate {

    Mono<String> templateMono(Object handleObject, ModelMap modelMap, String templateName);

    String template(Object handleObject, ModelMap modelMap, String templateName) throws IOException, TemplateException;
}
