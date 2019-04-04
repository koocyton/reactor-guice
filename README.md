# reactor-guice

Reactor-guice integrates the framework of Google Guice and Reactor-netty

```java
import com.doopp.kreactor.KReactorServer;
import com.github.pagehelper.PageInterceptor;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Names;
import com.doopp.gauss.server.database.HikariDataSourceProvider;
import com.doopp.gauss.server.module.ApplicationModule;
import com.doopp.gauss.server.module.RedisModule;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.mybatis.guice.MyBatisModule;
import org.mybatis.guice.datasource.helper.JdbcHelper;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class KTApplication {


    public static void main(String[] args) throws IOException {

        Properties properties = new Properties();
        properties.load(new FileInputStream(args[0]));

        // set you injector
        Injector injector = Guice.createInjector(

                // application Properties
                binder -> Names.bindProperties(binder, properties),

                // mybatis
                new MyBatisModule() {
                    @Override
                    protected void initialize() {
                        install(JdbcHelper.MySQL);
                        bindDataSourceProviderType(HikariDataSourceProvider.class);
                        // bindDataSourceProviderType(DruidDataSourceProvider.class);
                        bindTransactionFactoryType(JdbcTransactionFactory.class);
                        addMapperClasses("com.doopp.gauss.oauth.dao");
                        addInterceptorClass(PageInterceptor.class);
                        // Names.bindProperties(binder(), new ApplicationProperties());
                    }
                },

                // redis
                new RedisModule(),

                // application
                new ApplicationModule()
        );

        String host = properties.getProperty("server.host", "127.0.0.1");
        int port = Integer.valueOf(properties.getProperty("server.port", "8081"));

        // start server
        KReactorServer.create()
                .bind(host, port)
                .injector(injector)
                .basePackages("com.doopp.gauss.oauth.handle")
                .addFilter("/", new OAuthFilter(injector))
                .launch();
    }
}
```


Reactor-Guice scans the package path you specify and generates the Route path and websocket interface

#### Request Handle

```java
    @GET
    @Path("/test/json")
    @Produces({MediaType.APPLICATION_JSON})
    public Mono<Map<String, String>> testJson() {
        Map<String, String> map = new HashMap<>();
        return testService.serviceTest().map(s -> {
            map.put("doopp", s);
            return map;
        });
    }

    @GET
    @Path("/test/html/{id}")
    @Produces({MediaType.TEXT_HTML})
    public Mono<String> testHtml(@PathParam("id") Long id, ModelMap modelMap) {
        return Mono.just("test").map(s->{
            modelMap.addAttribute("id", id);
            return s;
        });
    }

    @GET
    @Path("/test/image")
    @Produces({"image/jpeg"})
    public Mono<ByteBuf> testImage() {
        return httpClient
            .get()
            .uri("https://static.cnbetacdn.com/article/2019/0402/6398390c491f650.jpg")
            .responseContent()
            .aggregate()
            .map(ByteBuf::retain);
    }
```


#### WebSocket

```java

@Path("/kreactor/ws")
@Singleton
public class WsTestHandle extends AbstractWebSocketServerHandle {

    @Override
    public void connected(Channel channel) {
        System.out.println(channel.id());
        super.connected(channel);
    }

    @Override
    public void onTextMessage(TextWebSocketFrame frame, Channel channel) {
        System.out.println(frame.text());
        super.onTextMessage(frame, channel);
    }
}

```