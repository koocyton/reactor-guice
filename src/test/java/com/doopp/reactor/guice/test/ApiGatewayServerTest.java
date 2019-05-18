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
        Properties properties = testProperties();

        Injector injector = Guice.createInjector(
            binder -> Names.bindProperties(binder, properties),
            new Module()
        );

        String host = injector.getInstance(Key.get(String.class, Names.named("server.host")));
        int port = injector.getInstance(Key.get(int.class, Names.named("server.port")));

        ReactorGuiceServer.create()
            .bind(host, port)
            // .injector(injector)
            // .setHttpMessageConverter(new JacksonHttpMessageConverter())
            .setApiGatewayDispatcher(new MyApiGatewayDispatcher())
            // .handlePackages("com.doopp.reactor.guice.test.handle")
            .addFilter("/", TestFilter.class)
            .launch();
    }

    private Properties testProperties() throws IOException {
        Properties properties = new Properties();
        // properties.load(new FileInputStream("D:\\project\\reactor-guice\\application.properties"));
        properties.load(new FileInputStream("/Developer/Project/reactor-guice/application.properties"));
        return properties;
    }
}
