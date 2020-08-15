# reactor-guice

Reactor-guice 是一个基于 Google Guice 和 Reactor-netty 的 Reactor 微服务框架

在开发中自动配置路由和依赖注入，以及用他作为你的网关服，仅在启动过程中使用反射来完善配置,

所以他的性能是基于 Guice 和 Reactor 模式。

#### Milestone
``` html
0.0.3 注册注解 @GET @POST @PUT @DELETE @Products @PATH
      支持 Websocket 可作为静态文件服
      支持对 URI 做 Filter 处理
      
0.0.5 静态文件目录后默认输出 index.html
      支持自选模板引擎，自带Freemark 和 Thymeleaf 处理类
      支持自定义Json引擎，自带 Gson 和jackson 的处理类
      添加 ModelMap, 注入到 Controlle 的方法，用户模板的变量代入
      可以上传文件了

0.0.8 可以 POST 数组
      通过返回 Mono.just("redirect:/...") 实现重定向
      支持 API 网关模式
      修复头信息错误的 BUG
      可选返回跨域头信息

0.10  支持输出 Protobuf
      BeanParm 可以支持 Form ,Json 或 Protobuf ，将上传的数据自动组织成对象
      上传的文件接受类型 byte[] , UploadFile, File
      上传文件可以自动保存到指定的目录

0.11  增加自动扫描 Controlle 类和 Service 类
      通过扫描完成路由配置和依赖注入，不同手动做额外配置

0.12.1 静态文件的读取塞入到异步中去处理
       这个版本是一个稳定的版本

0.12.2 优化 Jar 内读取文件的变量
       简化 websocket 的接口
       网关模式增加 websocket

0.12.4 增加 Https (0.12.3)
       静态文件目录可配置多个
       修复上传文件多次后，溢出的情况，补充 FileUpload.release()
       修复非 Form POST ，错误的做了 Bytebuf.release()

0.12.5 网关模式下，websocket 的线程安全处理

0.12.6 Guide 和 reactor-netty 版本更新

0.12.7 删除长连接不必要的 log
       增加 RedisModule

0.12.8 修复 JSON 传递的 BUG

0.12.9 WebSocket 增加支持 SecWebsocketPotocol

0.12.10 增加接口，用于初始化后立刻可用 injector

```

### 1. 引入 reactor-guice

#### maven
```
<dependency>
    <groupId>com.doopp</groupId>
    <artifactId>reactor-guice</artifactId>
    <version>0.12.8</version>
</dependency>
```

#### gradle
```
compile 'com.doopp:reactor-guice:0.12.8'
```

### 2. 创建应用

```java
public static void main(String[] args) throws IOException {
        // 载入配置
        Properties properties = new Properties();
        properties.load(new FileInputStream(args[0]));

        String host = properties.getProperty("server.host");
        int port = Integer.valueOf(properties.getProperty("server.port"));
        // 启动服务
        ReactorGuiceServer.create()
                .bind(host, port)
                .createInjector(
                        // 方便使用 @Names 来获取配置 
                        binder -> Names.bindProperties(binder, properties),
                        // 数据库
                        new MyBatisModule() {
                            @Override
                            protected void initialize() {
                                install(JdbcHelper.MySQL);
                                bindDataSourceProviderType(HikariDataSourceProvider.class);
                                bindTransactionFactoryType(JdbcTransactionFactory.class);
                                addMapperClasses("com.doopp.gauss.app.dao");
                                // addInterceptorClass(PageInterceptor.class);
                            }
                        },
                        // Redis    
                        new RedisModule() {
    
                            @Singleton
                            @Provides
                            @Named("userRedis")
                            public ShardedJedisHelper userRedis(JedisPoolConfig jedisPoolConfig,
                                                                @Named("redis.user.servers") String userServers) {
                                return new ShardedJedisHelper(userServers, jedisPoolConfig);
                            }
                        }
                        // 自定义的配置
                        new ApplicationModule()
                )
                // 配置 Json 处理类
                .setHttpMessageConverter(new MyGsonHttpMessageConverter())
                // 设定自动扫描 Controller 和 Service 的包名，可以配置多个
                .basePackages("com.doopp.gauss.app", ...)
                // 配置多个静态资源
                .addResource("/static/", "/static-public/")
                .addResource("/", "/public/")
                // https
                .setHttps(new File(jksFilePath), jksPassword, jksSecret)
                // 目前仅支持通过 URI 来过滤，可以多次 addFilter
                .addFilter("/", AppFilter.class)
                // 错误信息输出
                .printError(true)
                .launch();
    }
```

### 3. 创建 Controller

#### Controller Example

```java

@Controller
@Path("/api/admin")
public class ExampleController {

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

#### Api Gateway 模式

```java
ReactorGuiceServer.create()
        .bind(host, port)
        .setApiGatewayDispatcher(new MyApiGatewayDispatcher())
        .addFilter("/", TestFilter.class)
        .launch();
```

#### 混合的 Api Gateway Model

```java
ReactorGuiceServer.create()
        .bind(host, port)
        .createInjector(module1, module2, ...)
        .setHttpMessageConverter(new JacksonHttpMessageConverter())
        .setApiGatewayDispatcher(new MyApiGatewayDispatcher())
        .handlePackages("com.doopp.reactor.guice.test")
        .addFilter("/", TestFilter.class)
        .launch();
```

#### 表单和文件上传

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

#### Protobuf
```java
@GET
@Path("/test/protobuf")
@Produces("application/x-protobuf")
public Mono<byte[]> testProtobuf() {
    Hello.Builder builder = Hello.newBuilder();
    builder.setId(123);
    builder.setName("wuyi");
    builder.setEmail("wuyi@doopp.com");
    return Mono.just(builder.build().toByteArray());
}
```


