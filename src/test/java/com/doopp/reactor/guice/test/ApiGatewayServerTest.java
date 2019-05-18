package com.doopp.reactor.guice.test;

import com.doopp.reactor.guice.ReactorGuiceServer;
import com.doopp.reactor.guice.test.util.MyApiGatewayDispatcher;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ApiGatewayServerTest {

    @Test
    public void testApiGatewayModel() throws IOException {

        Properties properties = new Properties();
        properties.load(new FileInputStream("D:\\project\\reactor-guice\\application.properties"));
        // properties.load(new FileInputStream("/Developer/Project/reactor-guice/application.properties"));

        Injector injector = Guice.createInjector(
            binder -> Names.bindProperties(binder, properties),
            new Module()
        );

        ReactorGuiceServer.create()
            .bind("127.0.0.1", 8080)
            // .injector(injector)
            // .setHttpMessageConverter(new JacksonHttpMessageConverter())
            .setApiGatewayDispatcher(new MyApiGatewayDispatcher())
            // .handlePackages("com.doopp.reactor.guice.test.handle")
            .addFilter("/", TestFilter.class)
            .launch();
    }
}
