package com.doopp.reactor.guice.view;

public interface TemplateDelegate {

    String template(Object handleObject, ModelMap modelMap, String templateName);
}
