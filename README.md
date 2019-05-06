# reactor-guice

Reactor-guice integrates the framework of Google Guice and Reactor-netty

#### Milestone
``` html
0.0.3 Support annotations GET POST PUT DELETE
0.0.3 Support annotations Products PATH
0.0.3 Static File Support
0.0.3 Support Websocket
0.0.4 Custom filter by uri
0.0.5 index.html in the default output directory
0.0.5 Support for custom templates lib
      with gson and Jackson convert
0.0.5 Support for custom json lib
      with freeemark convert
      and modelmap for template
0.0.5 you can upload files
0.0.6 POST can be an array
0.0.7 use redirect:/** to redirect
0.0.8 support api gateway model
      fix Repeated header information

support protobuf
support udp server
maybe use Jersey to execute dispatch
```

### 1. import reactor-guice

#### maven
```
<dependency>
    <groupId>com.doopp</groupId>
    <artifactId>reactor-guice</artifactId>
    <version>0.0.7-SNAPSHOT</version>
</dependency>
```

#### gradle
```
compile 'com.doopp:reactor-guice:0.0.7-SNAPSHOT'
```

#### use Local Maven 
```
mvn clean

mvn package

mvn install:install-file -Dfile=target/reactor-guice-0.0.7.jar -DgroupId=com.doopp.local -DartifactId=reactor-guice -Dversion=0.0.7 -Dpackaging=jar

<dependency>
    <groupId>com.doopp.local</groupId>
    <artifactId>reactor-guice</artifactId>
    <version>0.0.7</version>
</dependency>
```

### 2. create you application

```java
Injector injector = Guice.createInjector(...);

ReactorGuiceServer.create()
    .bind(host, port)
    .injector(injector)
    .setHttpMessageConverter(new JacksonHttpMessageConverter())
    .setTemplateDelegate(new FreemarkTemplateDelegate())
    .handlePackages("com.doopp.reactor.guice.test.handle")
    .addFilter("/", TestFilter.class)
    .printError(true)
    .launch();
```

### 3. creat you service

#### Handle Example

```java
/** https://kreactor.doopp.com/test/json **/
@GET
@Path("/json")
@Produces({MediaType.APPLICATION_JSON})
public Mono<Map<String, String>> json() {
    return Mono
        .just(new HashMap<String, String>())
        .map(m -> {
            m.put("hi", "five girl");
            return m;
        });
}

/** https://kreactor.doopp.com/test/jpeg **/
@GET
@Path("/jpeg")
@Produces({"image/jpeg"})
public Mono<ByteBuf> jpeg() {
    return HttpClient.create()
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