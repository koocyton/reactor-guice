package com.doopp.reactor.guice.test;

import com.doopp.reactor.guice.ReactorGuiceServer;
import com.doopp.reactor.guice.json.JacksonHttpMessageConverter;
import com.doopp.reactor.guice.test.util.MyApiGatewayDispatcher;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.Properties;

public class ApiGatewayServer {

    @Test
    public void testApiGatewayModel() throws IOException, InterruptedException {

        Properties properties = new Properties();
        properties.load(new FileInputStream("D:\\project\\reactor-guice\\application.properties"));
        // properties.load(new FileInputStream("/Developer/Project/reactor-guice/application.properties"));

        String host = properties.getProperty("server.host", "127.0.0.1");
        int port = Integer.valueOf(properties.getProperty("server.port", "8081"));

        ReactorGuiceServer.create()
            .bind(host, port)
            .setHttpMessageConverter(new JacksonHttpMessageConverter())
            .createInjector(
                binder -> Names.bindProperties(binder, properties),
                new Module()
            )
            .setApiGatewayDispatcher(new MyApiGatewayDispatcher())
            .basePackages("com.doopp.reactor.guice.test")
            .addFilter("/", TestFilter.class)
            .printError(true)
            .launch();
    }
}
