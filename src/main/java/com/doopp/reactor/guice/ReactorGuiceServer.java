package com.doopp.reactor.guice;

import com.doopp.reactor.guice.annotation.Controller;
import com.doopp.reactor.guice.json.HttpMessageConverter;
import com.doopp.reactor.guice.publisher.*;
import com.doopp.reactor.guice.view.TemplateDelegate;
import com.doopp.reactor.guice.websocket.AbstractWebSocketServerHandle;
import com.doopp.reactor.guice.websocket.WebSocketServerHandle;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import io.netty.channel.ChannelOption;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.*;

import javax.net.ssl.KeyManagerFactory;
import javax.ws.rs.*;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.KeyStore;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class ReactorGuiceServer {

    private String host = "127.0.0.1";

    private int port = 8083;

    private int sslPort = 8084;

    final private String version = "0.12.10";

    // handle
    private HandlePublisher handlePublisher = new HandlePublisher();

    // static file publisher
    private StaticFilePublisher staticFilePublisher = new StaticFilePublisher();

    // websocket
    private WebsocketPublisher websocketPublisher = new WebsocketPublisher();

    private Injector injector;

    // error log print
    private boolean printError = false;

    // Cross-Origin Resource Sharing
    private boolean crossOrigin = false;

    // api gateway model default disabled
    private ApiGatewayDispatcher apiGatewayDispatcher;

    private final Map<String, Class<? extends Filter>> filters = new HashMap<>();

    private final Set<String> basePackages = new HashSet<>();

    private final Set<Module> modules = new HashSet<>();

    static final Set<String> classNames = new HashSet<>();

    private ScheduledExecutorService newScheduledThreadPool = null;

    private SslContext sslContext = null;

    private static Map<String, FileSystem> jarPathFS = new HashMap<>();

    public static ReactorGuiceServer create() {
        return new ReactorGuiceServer();
    }

    public ReactorGuiceServer bind(String host, int port) {
        this.host = host;
        this.port = port;
        return this;
    }

    public ReactorGuiceServer bind(String host, int port, int sslPort) {
        this.host = host;
        this.port = port;
        this.sslPort = sslPort;
        return this;
    }

    public ReactorGuiceServer basePackages(String... basePackages) {
        Collections.addAll(this.basePackages, basePackages);
        this.setClassNames();
        return this;
    }

    public ReactorGuiceServer createInjector(Module... modules) {
        Collections.addAll(this.modules, modules);
        return this;
    }

    public ReactorGuiceServer importInjector(Injector injector) {
        assert injector!=null : "A Injector instance is required";
        this.injector = injector;
        return this;
    }

    public ReactorGuiceServer addFilter(String path, Class<? extends Filter> clazz) {
        this.filters.put(path, clazz);
        return this;
    }

    public ReactorGuiceServer addResource(String uriBegin, String resourcePath) {
        staticFilePublisher.addResourceLocations(uriBegin, resourcePath);
        return this;
    }

    public ReactorGuiceServer setHttpMessageConverter(HttpMessageConverter httpMessageConverter) {
        assert httpMessageConverter!=null : "A HttpMessageConverter instance is required";
        handlePublisher.setHttpMessageConverter(httpMessageConverter);
        return this;
    }

    public ReactorGuiceServer setTemplateDelegate(TemplateDelegate templateDelegate) {
        assert templateDelegate!=null : "A TemplateDelegate instance is required";
        handlePublisher.setTemplateDelegate(templateDelegate);
        return this;
    }

    public ReactorGuiceServer setApiGatewayDispatcher(ApiGatewayDispatcher apiGatewayDispatcher) {
        assert apiGatewayDispatcher!=null : "A ApiGatewayDispatcher instance is required";
        this.apiGatewayDispatcher = apiGatewayDispatcher;
        return this;
    }

    public ReactorGuiceServer printError(boolean printError) {
        this.printError = printError;
        return this;
    }

    public ReactorGuiceServer crossOrigin (boolean crossOrigin) {
        this.crossOrigin = crossOrigin;
        return this;
    }

    public ReactorGuiceServer setHttps (File jksFile, String jksPassword, String jksSecret) {

        try {
            // ssl
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(new FileInputStream(jksFile), jksPassword.toCharArray());
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, jksSecret.toCharArray());
            sslContext = SslContextBuilder.forServer(keyManagerFactory).build();
        }
        catch(Exception e) {
            e.printStackTrace();
            sslContext = null;
        }

        // SelfSignedCertificate cert = new SelfSignedCertificate();
        // SslContextBuilder serverOptions = SslContextBuilder.forServer(cert.certificate(), cert.privateKey());

        return this;
    }

    public ReactorGuiceServer setTestHttps () {

        try {
            SelfSignedCertificate cert = new SelfSignedCertificate();
            SslContextBuilder serverOptions = SslContextBuilder.forServer(cert.certificate(), cert.privateKey());
            sslContext = serverOptions.build();

        }
        catch(Exception e) {
            e.printStackTrace();
            sslContext = null;
        }

        // SelfSignedCertificate cert = new SelfSignedCertificate();
        // SslContextBuilder serverOptions = SslContextBuilder.forServer(cert.certificate(), cert.privateKey());

        return this;
    }

    private final List<Consumer<Injector>> afterConsumerList = new ArrayList<>();

    public ReactorGuiceServer createInjectorAfter(Consumer<Injector> injectorConsumer) {
        afterConsumerList.add(injectorConsumer);
        return this;
    }

    public void launch() {

        // 如果 injector 没有设定，就使用自动扫描的注入
        if (this.injector==null) {
            modules.add(new AutoImportModule());
            this.injector = Guice.createInjector(modules);
        }

        if (afterConsumerList.size()>0) {
            afterConsumerList.forEach(injectorConsumer -> {
                injectorConsumer.accept(this.injector);
            });
        }

        Consumer<HttpServerRoutes> httpServerRoutesConsumer = this.routesBuilder();

        // 启动服务
        DisposableServer disposableServer = HttpServer.create()
            .tcpConfiguration(tcpServer -> {
                if (sslContext==null) {
                    return tcpServer.option(ChannelOption.SO_KEEPALIVE, true)
                        // .secure(s -> s.sslContext(sslContext))
                        // .option(ChannelOption.SO_BACKLOG, 128)
                        .host(this.host)
                        .port(this.port);
                }
                else {
                    return tcpServer.option(ChannelOption.SO_KEEPALIVE, true)
                        .secure(s -> s.sslContext(sslContext))
                        // .option(ChannelOption.SO_BACKLOG, 128)
                        .host(this.host)
                        .port(this.sslPort);
                }
            })
            .route(httpServerRoutesConsumer)
            // .host(this.host)
            // .port(this.port)
            .wiretap(true)
            .bindNow();

//        if (sslContext!=null) {
//            DisposableServer sslDisposableServer = HttpServer.create()
//                    .tcpConfiguration(tcpServer -> {
//                        if (sslContext!=null) {
//                            return tcpServer.option(ChannelOption.SO_KEEPALIVE, true)
//                                    .secure(s -> s.sslContext(sslContext))
//                                    .host(this.host)
//                                    .port(this.sslPort);
//                        }
//                        return tcpServer;
//                    })
//                    .route(httpServerRoutesConsumer)
//                    // .host(this.host)
//                    // .port(this.port)
//                    .wiretap(true)
//                    .bindNow();
//
//            Channel ch80 = disposableServer.channel().ss;
//            Channel ch443 = sslDisposableServer.channel();
//
//            ch80.closeFuture().sync();
//            ch443.closeFuture().sync();
//        }

        System.out.printf("\n>>> KReactor Server Running http://%s:%d/ ... \n\n", this.host, this.port);

        disposableServer.onDispose().block();
    }

    private Consumer<HttpServerRoutes> routesBuilder() {
        // routes
        return routes -> {
            for (String className : classNames) {
                if (injector == null) {
                    continue;
                }
                Object handleObject;
                Class<?> handleClass;
                try {
                    handleClass = Class.forName(className);
                    if (handleClass.isInterface()) {
                        continue;
                    }
                }
                catch(ClassNotFoundException e) {
                    continue;
                }
                // 拿到根路径
                Path pathAnnotation = handleClass.getAnnotation(Path.class);
                String rootPath = (pathAnnotation == null) ? "" : pathAnnotation.value();
                // if websocket
                if (AbstractWebSocketServerHandle.class.isAssignableFrom(handleClass) && pathAnnotation != null) {
                    handleObject = injector.getInstance(handleClass);
                    System.out.println("    WS " + rootPath + " -> " + className);
                    routes.get(rootPath, (req, resp) -> httpPublisher(req, resp, null, o ->
                            websocketPublisher.sendMessage(req, resp, (WebSocketServerHandle) handleObject, o)
                    ));
                    continue;
                }
                // if is not controller
                if (!handleClass.isAnnotationPresent(Controller.class)) {
                    continue;
                }
                handleObject = injector.getInstance(handleClass);
                // methods for handle
                Method[] handleMethods = handleClass.getMethods();
                // loop methods
                for (Method method : handleMethods) {
                    // if have request path
                    if (method.isAnnotationPresent(Path.class)) {
                        String requestPath = rootPath + method.getAnnotation(Path.class).value();
                        // GET
                        if (method.isAnnotationPresent(GET.class)) {
                            System.out.println("   GET " + requestPath + " -> " + className + ":" + method.getName());
                            routes.get(requestPath, (req, resp) -> httpPublisher(req, resp, method, o ->
                                handlePublisher.sendResult(req, resp, method, handleObject, o)
                            ));
                        }
                        // POST
                        else if (method.isAnnotationPresent(POST.class)) {
                            System.out.println("  POST " + requestPath + " -> " + className + ":" + method.getName());
                            routes.post(requestPath, (req, resp) -> httpPublisher(req, resp, method, o ->
                                handlePublisher.sendResult(req, resp, method, handleObject, o)
                            ));
                        }
                        // DELETE
                        else if (method.isAnnotationPresent(DELETE.class)) {
                            System.out.println("DELETE " + requestPath + " -> " + className + ":" + method.getName());
                            routes.delete(requestPath, (req, resp) -> httpPublisher(req, resp, method, o ->
                                handlePublisher.sendResult(req, resp, method, handleObject, o)
                            ));
                        }
                        // UPDATE
                        else if (method.isAnnotationPresent(PUT.class)) {
                            System.out.println("   PUT " + requestPath + " -> " + className + ":" + method.getName());
                            routes.put(requestPath, (req, resp) -> httpPublisher(req, resp, method, o ->
                                handlePublisher.sendResult(req, resp, method, handleObject, o)
                            ));
                        }
                        if (crossOrigin) {
                            // OPTION
                            routes.options(requestPath, (req, resp) -> httpPublisher(req, resp, method, o -> Mono.empty()));
                        }
                    }
                }
            }

            // is is api gateway server
            if (this.apiGatewayDispatcher!=null) {
                ApiGatewayPublisher apiGatewayPublisher = new ApiGatewayPublisher(this.apiGatewayDispatcher);
                System.out.println("   Any /** ->  /** <api gateway model>");
                routes.route(apiGatewayPublisher::checkRequest, (req, resp) -> httpPublisher(req, resp, null, o ->
                    apiGatewayPublisher.sendResponse(req, resp, websocketPublisher, o)
                ));
            }
            // static server
            else {
                System.out.println("   GET /** ->  /public/* <static files>");
                routes.route(req->true, (req, resp) -> httpPublisher(req, resp, null, o ->
                    staticFilePublisher.sendFile(req, resp)
                ));
            }
        };
    }

    private Publisher<Void> httpPublisher(HttpServerRequest req, HttpServerResponse resp, Method method, Function<Object, Mono<?>> handle) {

        // response header
        if (req.isKeepAlive()) {
            resp.header(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }
        resp.header(HttpHeaderNames.SERVER, "RGS/" + this.version);

        // cross domain
        if (crossOrigin) {
            resp.header("Access-Control-Allow-Origin", "*")
                        .header("Access-Control-Allow-Headers", "X-Requested-With, accept, origin, content-type")
                        .header("Access-Control-Allow-Methods", "PUT,POST,GET,DELETE,OPTIONS");
        }

        // result
        return doFilter(req, resp, new RequestAttribute())
            .flatMap(handle)
            .onErrorMap(throwable -> {
                if (this.printError) {
                    throwable.printStackTrace();
                }
                // if handle @Products is json  , RETURN StatusMessageException
                if (handlePublisher.methodProductsValue(method).contains(MediaType.APPLICATION_JSON)) {
                    if (throwable instanceof StatusMessageException) {
                        resp.status(((StatusMessageException) throwable).getCode());
                        resp.header(HttpHeaderNames.CONTENT_TYPE, MediaType.APPLICATION_JSON);
                        return throwable;
                    }
                    else {
                        resp.status(HttpResponseStatus.INTERNAL_SERVER_ERROR);
                        resp.header(HttpHeaderNames.CONTENT_TYPE, MediaType.APPLICATION_JSON);
                        return new StatusMessageException(HttpResponseStatus.INTERNAL_SERVER_ERROR, throwable.getMessage());
                    }
                }
                // else return other Exception
                else {
                    if (throwable instanceof StatusMessageException) {
                        resp.status(((StatusMessageException) throwable).getCode());
                        resp.header(HttpHeaderNames.CONTENT_TYPE, MediaType.TEXT_PLAIN);
                        return new Exception(throwable.getMessage());
                    }
                    else {
                        resp.status(HttpResponseStatus.INTERNAL_SERVER_ERROR);
                        resp.header(HttpHeaderNames.CONTENT_TYPE, MediaType.TEXT_PLAIN);
                        return throwable;
                    }
                }
            })
            .onErrorResume(throwable -> {
                // is json
                if (throwable instanceof StatusMessageException) {
                    if (handlePublisher.getHttpMessageConverter()==null) {
                        return Mono.just("{\"err_code\":500, \"err_msg\":\"A Message Converter instance is required\", \"data\":null}");
                    }
                    return Mono.just(
                        handlePublisher.getHttpMessageConverter().toJson(throwable)
                    );
                }
                // string
                else {
                    return Mono.just(throwable.getMessage());
                }
            })
            .flatMap(o -> {
                if (o instanceof Mono<?>) {
                    return ((Mono<?>) o).then();
                }
                return (o instanceof String)
                                ? resp.sendString(Mono.just((String) o)).then()
                                : resp.sendObject(Mono.just(o)).then();
            });
    }

    private Mono<Object> doFilter(HttpServerRequest req, HttpServerResponse resp, RequestAttribute requestAttribute) {
        // loop filter map
        for (String key : this.filters.keySet()) {
            // choice filter
            if (req.uri().length() >= key.length() && req.uri().startsWith(key)) {
                return this.injector.getInstance(this.filters.get(key)).doFilter(req, resp, requestAttribute);
            }
        }
        return Mono.just(requestAttribute);
    }

//    private File getUriFile(URI uri) {
//        final java.nio.file.Path resource;
//        if (uri.toString().startsWith("jar:")) {
//            Map<String, String> env = new HashMap<>();
//            String[] array = uri.toString().split("!");
//            FileSystem fs = FileSystems.newFileSystem(URI.create(array[0]), env);
//            resource = fs.getPath(array[1]);
//            fs.close();
//        } else {
//            resource = Paths.get(uri);
//        }
//        return resource.toFile();
//    }

    private void setClassNames() {
        // Set<String> handleClasses = new HashSet<>();
        for(String basePackage : basePackages) {
            try {
                // URL resource = this.getClass().getResource("/" + basePackage.replace(".", "/"));
                // System.out.println("resource.toString() : " + resource.toString());
                // System.out.println("resource.toURI() : " + resource.toURI());
                // System.out.println("resource.getFile() : " + resource.getFile());
                // System.out.println("resource.getPath() : " + resource.getPath());
                java.nio.file.Path resourcePath = classResourcePath("/" + basePackage.replace(".", "/")).block();
                if (resourcePath==null) {
                    continue;
                }
                // System.out.println(resourcePath);
                Files.walkFileTree(resourcePath, new SimpleFileVisitor<java.nio.file.Path>() {
                    @Override
                    public FileVisitResult visitFile(java.nio.file.Path file, BasicFileAttributes attrs) {
                        String uriPath = file.toUri().toString();
                        // System.out.println("file.toUri() : " + file.toUri());
                        // System.out.println("file.toRealPath() : " + file.toRealPath());
                        // System.out.println("file.toAbsolutePath() : " + file.toAbsolutePath());
                        if (uriPath.endsWith(".class")) {
                            int startIndexOf = uriPath.indexOf(basePackage.replace(".", "/"));
                            int endIndexOf = uriPath.indexOf(".class");
                            if (startIndexOf<0) {
                                return FileVisitResult.CONTINUE;
                            }
                            String classPath = uriPath.substring(startIndexOf, endIndexOf);
                            String className = classPath.replace("/", ".");
                            classNames.add(className);
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
            catch(Exception e) {
                e.printStackTrace();
            }
        }
//        Set<String> handleClasses = new HashSet<>();
//        Set<File> dirList = new HashSet<>();
//        for(String basePackage : basePackages) {
//            URL scanUrl = this.getClass().getResource("/" + basePackage.replace(".", "/"));
//            File uriFile = getUriFile(scanUrl.toURI());
//            if (uriFile.isDirectory()) {
//                dirList.add(uriFile);
//            }
//            int ii=1;
//            while(true) {
//                File ff = dirList.iterator().next();
//                File[] fff = ff.listFiles();
//                if (fff==null) {
//                    continue;
//                }
//                for (File file : fff) {
//                    if (file.isDirectory()) {
//                        dirList.add(file);
//                        ii++;
//                    }
//                    else if (file.getName().endsWith(".class")) {
//                        handleClasses.add(file.getName());
//                    }
//                }
//                if (ii>=dirList.size()) {
//                    break;
//                }
//            }
//        }
//        return handleClasses;

//        URL scanUrl = this.getClass().getResource("/" + this.basePackages.iterator().next().replace(".", "/"));
//
//        // init result
//        Set<String> handleClassesName = new HashSet<>();
//        if (this.handlePackages.size()<1) {
//            return handleClassesName;
//        }
//        URL resourceUrl = this.getClass().getResource("/" + this.handlePackages.iterator().next().replace(".", "/"));
//        if (resourceUrl==null) {
//            return handleClassesName;
//        }
//        String resourcePath = resourceUrl.getPath();
//        // if is jar package
//        if (resourcePath.contains(".jar!")) {
//            try (JarFile jarFile = new JarFile(resourcePath.substring(5, resourcePath.lastIndexOf(".jar!") + 4))) {
//                Enumeration<JarEntry> entries = jarFile.entries();
//                while (entries.hasMoreElements()) {
//                    JarEntry jar = entries.nextElement();
//                    String name = jar.getName();
//                    for (String packageName : this.handlePackages) {
//                        if (name.contains(packageName.replace(".", "/")) && name.contains(".class")) {
//                            int beginIndex = packageName.length() + 1;
//                            int endIndex = name.lastIndexOf(".class");
//                            String className = name.substring(beginIndex, endIndex);
//                            handleClassesName.add(packageName + "." + className);
//                        }
//                    }
//                }
//            }
//            catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//        // if is dir
//        else {
//            for (String packageName : this.handlePackages) {
//                String path = "/" + packageName.replace(".", "/");
//                resourcePath = this.getClass().getResource(path).getPath();
//                File dir = new File(resourcePath);
//                File[] files = dir.listFiles();
//                if (files == null) {
//                    continue;
//                }
//                for (File file : files) {
//                    String name = file.getName();
//                    int endIndex = name.lastIndexOf(".class");
//                    String className = name.substring(0, endIndex);
//                    handleClassesName.add(packageName + "." + className);
//                }
//            }
//        }
//        return handleClassesName;
    }

    // private static FileSystem classFs = null;

    public static Mono<java.nio.file.Path> classResourcePath(String resourceUri) {
        URL resource = ReactorGuiceServer.class.getResource(resourceUri);
        return Mono.fromCallable(()->{
            if (resource.getProtocol().equals("jar")) {
                String[] jarPathInfo = resource.getPath().split("!");
                if (jarPathInfo[0].startsWith("file:")) {
                    jarPathInfo[0] = java.io.File.separator.equals("\\")
                            ? jarPathInfo[0].substring(6)
                            : jarPathInfo[0].substring(5);
                }
                if (jarPathFS.get(jarPathInfo[0])==null || !jarPathFS.get(jarPathInfo[0]).isOpen()) {
                    java.nio.file.Path jarPath = Paths.get(jarPathInfo[0]);
                    jarPathFS.put(jarPathInfo[0], FileSystems.newFileSystem(jarPath, null));
                }
                return jarPathFS.get(jarPathInfo[0]).getPath(jarPathInfo[1]);
            }
            return Paths.get(resource.toURI());
        })
            .subscribeOn(Schedulers.elastic())
            .onErrorResume(t->Mono.error(new StatusMessageException(404, "Not Found")));
    }
}
