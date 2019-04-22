package com.doopp.reactor.guice.common;

import freemarker.template.*;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class FreemarkTemplateDelegate implements TemplateDelegate {

    private Configuration configuration = templateConfiguration();

    // 输出模版
    @Override
    public String template(Object handleObject, ModelMap modelMap, String templateName) throws IOException, TemplateException {
        String controllerName = handleObject.getClass().getSimpleName();
        String templateDirectory = controllerName.toLowerCase().substring(0, controllerName.length()-"handle".length());
        this.configuration.setClassForTemplateLoading(handleObject.getClass(), "/template/" + templateDirectory);
        Template template = this.configuration.getTemplate(templateName + ".html");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        template.process(modelMap, new OutputStreamWriter(outputStream));
        return outputStream.toString("UTF-8");
    }

    // 输出模版
    @Override
    public Mono<String> templateMono(Object handleObject, ModelMap modelMap, String templateName) {
        return Mono.create(slink -> {
            try {
                slink.success(this.template(handleObject, modelMap, templateName));
            }
            catch(Exception e) {
                slink.error(e);
            }
        });
    }

    // 配置模版
    private Configuration templateConfiguration() {
        Version version = new Version("2.3.28");
        DefaultObjectWrapperBuilder defaultObjectWrapperBuilder = new DefaultObjectWrapperBuilder(version);

        Configuration cfg = new Configuration(version);
        cfg.setObjectWrapper(defaultObjectWrapperBuilder.build());
        cfg.setDefaultEncoding("UTF-8");
        // Don't log exceptions inside FreeMarker that it will thrown at you anyway:
        cfg.setLogTemplateExceptions(false);
        // Sets how errors will appear. Here we assume we are developing HTML pages.
        // For production systems TemplateExceptionHandler.RETHROW_HANDLER is better.
        // During web page *development* TemplateExceptionHandler.HTML_DEBUG_HANDLER is better.
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);
        cfg.setClassForTemplateLoading(this.getClass(), "/template");
        // Bind instance for DI
        // bind(Configuration.class).toInstance(cfg);
        return cfg;
    }
}
