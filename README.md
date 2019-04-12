# reactor-guice

Reactor-guice integrates the framework of Google Guice and Reactor-netty


### 1. import reactor-guice

#### maven
```
<dependency>
    <groupId>com.doopp</groupId>
    <artifactId>reactor-guice</artifactId>
    <version>0.0.5</version>
</dependency>
```

#### gradle
```
compile 'com.doopp:reactor-guice:0.0.5'
```

#### use Local Maven 
```
mvn clean

mvn package

mvn install:install-file -Dfile=target/reactor-guice-0.0.5.jar -DgroupId=com.doopp.local -DartifactId=reactor-guice -Dversion=0.0.5 -Dpackaging=jar

<dependency>
    <groupId>com.doopp.local</groupId>
    <artifactId>reactor-guice</artifactId>
    <version>0.0.5</version>
</dependency>
```

### 2. create you application

```java
Injector injector = Guice.createInjector(...);

KReactorServer.create()
        .bind(host, port)
        .injector(injector)
        .handlePackages("you_handle_package")
        // .addFilter("/", AppFilter.class)
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