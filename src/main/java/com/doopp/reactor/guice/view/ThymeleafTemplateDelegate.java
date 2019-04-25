package com.doopp.reactor.guice.view;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.FileTemplateResolver;

public class ThymeleafTemplateDelegate implements TemplateDelegate {

    @Override
    public String template(Object handleObject, ModelMap modelMap, String templateName) {

        String controllerName = handleObject.getClass().getSimpleName();
        String templateDirectory = controllerName.toLowerCase().substring(0, controllerName.length() - "handle".length());

        FileTemplateResolver templateResolver = new FileTemplateResolver();
        templateResolver.setPrefix("classpath:/templates/" + templateDirectory);
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setCacheable(false);
        templateResolver.setTemplateMode("HTML5");

        TemplateEngine engine = new TemplateEngine();
        engine.setTemplateResolver(templateResolver);

        Context context = new Context();
        context.setVariables(modelMap);

        return engine.process(templateName, context);
    }
}
