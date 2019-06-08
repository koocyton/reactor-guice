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
      add cross domain header and options request
0.0.9 support protobuf ( can output byte[] )
      fix redirect bug ... -_-
      the default JSON output returns the execution
          result directly ( remove object wrapping )
0.10  support BeanParam for form data , json data and protobuf data
      upload file recive support byte[] UploadFile File annotation
      upload file use File type can auto save specified directory
      add more test example 
0.11  autoscan controller Class than add custom mothd to route
      (Remove handlePackage , add basePackage to create server Method)
      autoscan service class ,then inject Guice model

support udp server
maybe use Jersey to execute dispatch
```

### 1. import reactor-guice

#### maven
```
<dependency>
    <groupId>com.doopp</groupId>
    <artifactId>reactor-guice</artifactId>
    <version>0.11</version>
</dependency>
```

#### gradle
```
compile 'com.doopp:reactor-guice:0.11'
```

### 2. create you application

```java
// Injector injector = Guice.createInjector(...);

ReactorGuiceServer.create()
    .bind(host, port)
    // .injector(injector)
    .createInjector(Module... mudules)
    .setHttpMessageConverter(new JacksonHttpMessageConverter())
    .setTemplateDelegate(new FreemarkTemplateDelegate())
    .handlePackages("com.doopp.reactor.guice.test")
    .addFilter("/", TestFilter.class)
    .crossOrigin(true)
    .printError(true)
    .launch();
```

### 3. creat you service

#### Handle Example

```java
@Controller
class ApplicationController {
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

#### Api Gateway Model

```java
ReactorGuiceServer.create()
        .bind(host, port)
        .setApiGatewayDispatcher(new MyApiGatewayDispatcher())
        .addFilter("/", TestFilter.class)
        .launch();
```

#### Mixed Api Gateway Model

```java
ReactorGuiceServer.create()
        .bind(host, port)
        // .injector(injector)
        .createInjector(Module... mudules)
        .setHttpMessageConverter(new JacksonHttpMessageConverter())
        .setApiGatewayDispatcher(new MyApiGatewayDispatcher())
        .handlePackages("com.doopp.reactor.guice.test")
        .addFilter("/", TestFilter.class)
        .launch();
```

#### Receive file update and form post

```java

// Server
@POST
@Path("/test/post-bean")
public Mono<User> testPostBean(@BeanParam User user, @FileParam(value="image", path = "C:\\Users\\koocyton\\Desktop") File[] file) {
    return Mono.just(user);
}

// Client Test
@Test
public void testFileUpload() {

    String hhe = HttpClient.create()
        .post()
        .uri("http://127.0.0.1:8083/kreactor/test/post-bean")
        .sendForm((req, form) -> form.multipart(true)
            .attr("id", "123123121312312")
            .attr("account", "account")
            .attr("password", "password")
            .attr("name", "name")
            .file("image", new File("C:\\Users\\koocyton\\Pictures\\cloud.jpg"))
            .file("image", new File("C:\\Users\\koocyton\\Pictures\\st.jpg"))
        )
        .responseSingle((res, content) -> content)
        .map(byteBuf -> byteBuf.toString(CharsetUtil.UTF_8))
        .block();

    System.out.println(hhe);
}
```