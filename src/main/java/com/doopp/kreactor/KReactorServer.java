package com.doopp.kreactor;

import com.doopp.kreactor.common.KReactorException;
import com.doopp.kreactor.common.KReactorFilter;
import com.doopp.kreactor.common.RequestAttribute;
import com.doopp.kreactor.publisher.HandlePublisher;
import com.doopp.kreactor.publisher.StaticFilePublisher;
import com.doopp.kreactor.publisher.WebsocketPublisher;
import com.doopp.kreactor.websocket.AbstractWebSocketServerHandle;
import com.doopp.kreactor.websocket.WebSocketServerHandle;
import com.google.inject.Injector;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import reactor.netty.http.server.HttpServerRoutes;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class KReactorServer {

    private static final KReactorServer INSTANCE = new KReactorServer();

    private static final HandlePublisher handlePublisher = new HandlePublisher();

    private static final StaticFilePublisher staticFilePublisher = new StaticFilePublisher();

    private static final WebsocketPublisher websocketPublisher = new WebsocketPublisher();

    private String host = "127.0.0.1";

    private int port = 8081;

    private Injector injector;

    private final Map<String, KReactorFilter> filters = new HashMap<>();

    private final Set<String> handlePackages = new HashSet<>();

    public static KReactorServer create() {
        return INSTANCE;
    }

    public KReactorServer bind(String host, int port) {
        this.host = host;
        this.port = port;
        return this;
    }

    public KReactorServer basePackages(String... basePackages) {
        Collections.addAll(this.handlePackages, basePackages);
        return this;
    }

    public KReactorServer injector(Injector injector) {
        this.injector = injector;
        return this;
    }

    public KReactorServer addFilter(String path, KReactorFilter filter) {
        this.filters.put(path, filter);
        return this;
    }

    public void launch() {
        DisposableServer disposableServer = HttpServer.create()
            .route(this.routesBuilder())
            .host(this.host)
            .port(this.port)
            .wiretap(true)
            .bindNow();

        System.out.println("\n>>> KReactor Server Running... ");

        disposableServer.onDispose().block();
    }

    private Consumer<HttpServerRoutes> routesBuilder() {

        return routes -> {
            Set<String> handleClassesName = this.getHandleClassesName();
            for (String handleClassName : handleClassesName) {
                if (injector == null) {
                    continue;
                }
                Object handleObject;
                // 如果初始化对象有问题
                try {
                    handleObject = injector.getInstance(Class.forName(handleClassName));
                } catch (Exception e) {
                    continue;
                }
                // 如果是静态方法，或接口
                int classModifiers = handleObject.getClass().getModifiers();
                if (Modifier.isAbstract(classModifiers) || Modifier.isInterface(classModifiers)) {
                    continue;
                }
                // 拿到根路径
                Path pathAnnotation = handleObject.getClass().getAnnotation(Path.class);
                String rootPath = (pathAnnotation == null) ? "" : pathAnnotation.value();
                // if websocket
                if (AbstractWebSocketServerHandle.class.isAssignableFrom(handleObject.getClass()) && pathAnnotation != null) {
                    System.out.println("    WS " + rootPath + " → " + handleClassName);
                    routes.get(rootPath, (req, resp) -> httpPublisher(req, resp, o ->
                            websocketPublisher.sendMessage(req, resp, (WebSocketServerHandle) handleObject, o)
                    ));
                    continue;
                }
                // methods for handle
                Method[] handleMethods = handleObject.getClass().getMethods();
                // loop methods
                for (Method method : handleMethods) {
                    // if have request path
                    if (method.isAnnotationPresent(Path.class)) {
                        String requestPath = rootPath + method.getAnnotation(Path.class).value();
                        // GET
                        if (method.isAnnotationPresent(GET.class)) {
                            System.out.println("   GET " + requestPath + " → " + handleClassName + ":" + method.getName());
                            routes.get(requestPath, (req, resp) -> httpPublisher(req, resp, o ->
                                handlePublisher.sendResult(req, resp, method, handleObject, o)
                            ));
                        }
                        // POST
                        else if (method.isAnnotationPresent(POST.class)) {
                            System.out.println("  POST " + requestPath + " → " + handleClassName + ":" + method.getName());
                            routes.post(requestPath, (req, resp) -> httpPublisher(req, resp, o ->
                                handlePublisher.sendResult(req, resp, method, handleObject, o)
                            ));
                        }
                        // DELETE
                        else if (method.isAnnotationPresent(DELETE.class)) {
                            System.out.println("DELETE " + requestPath + " → " + handleClassName + ":" + method.getName());
                            routes.delete(requestPath, (req, resp) -> httpPublisher(req, resp, o ->
                                handlePublisher.sendResult(req, resp, method, handleObject, o)
                            ));
                        }
                        // UPDATE
                        else if (method.isAnnotationPresent(PUT.class)) {
                            System.out.println("   PUT " + requestPath + " → " + handleClassName + ":" + method.getName());
                            routes.put(requestPath, (req, resp) -> httpPublisher(req, resp, o ->
                                handlePublisher.sendResult(req, resp, method, handleObject, o)
                            ));
                        }
                    }
                }
            }
            System.out.println("   GET /** →  /public/* <static files>");
            routes.get("/**", (req, resp) -> httpPublisher(req, resp, o ->
                    staticFilePublisher.sendFile(req, resp)
                )
            );
        };
    }

    private Publisher<Void> httpPublisher(HttpServerRequest req, HttpServerResponse resp, Function<Object, Mono<Object>> handle) {
        return doFilter(req, resp, new RequestAttribute())
            .flatMap(handle)
            .onErrorResume(throwable ->
                (throwable instanceof KReactorException)
                    ? Mono.just(throwable)
                    : Mono.just(new KReactorException(HttpResponseStatus.INTERNAL_SERVER_ERROR, throwable.getMessage()))
            )
            .flatMap(o -> {
                if (o instanceof KReactorException) {
                    KReactorException ke = (KReactorException) o;
                    return resp
                        .addHeader(HttpHeaderNames.SERVER, "power by reactor")
                        .addHeader(HttpHeaderNames.CONTENT_TYPE, MediaType.TEXT_HTML)
                        .status(ke.getCode())
                        .sendString(
                            Mono.just(ke.getMessage())
                        ).then();
                } else if (o instanceof String) {
                    return resp
                        .addHeader(HttpHeaderNames.SERVER, "power by reactor")
                        .status(HttpResponseStatus.OK)
                        .sendString(
                            Mono.just((String) o)
                        ).then();
                } else {
                    return resp
                        .addHeader(HttpHeaderNames.SERVER, "power by reactor")
                        .status(HttpResponseStatus.OK)
                        .sendObject(
                            Mono.just(o)
                        ).then();
                }
            });
    }


    /**
     * 处理 filter
     *
     * @param req              HttpServerRequest
     * @param resp             HttpServerResponse
     * @param requestAttribute 当次请求
     * @return Mono
     */
    private Mono<Object> doFilter(HttpServerRequest req, HttpServerResponse resp, RequestAttribute requestAttribute) {
        for (String key : this.filters.keySet()) {
            if (req.uri().length() >= key.length() && req.uri().substring(0, key.length()).equals(key)) {
                return this.filters.get(key).doFilter(req, resp, requestAttribute);
            }
        }
        return Mono.just(requestAttribute);
    }

    /**
     * 获取 handle 的类名
     *
     * @return Set<String>
     */
    private Set<String> getHandleClassesName() {
        // init result
        Set<String> handleClassesName = new HashSet<>();
        // loop search handle
        for (String packageName : this.handlePackages) {
            String path = "/" + packageName.replace(".", "/");
            String resourcePath = this.getClass().getResource(path).getPath();
            // if in jar
            if (resourcePath.contains(".jar")) {
                JarFile jarFile;
                try {
                    jarFile = new JarFile(resourcePath.substring(5, resourcePath.lastIndexOf(".jar!") + 4));
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry jar = entries.nextElement();
                    String name = jar.getName();
                    if (name.contains(packageName.replace(".", "/")) && name.contains(".class")) {
                        int beginIndex = packageName.length() + 1;
                        int endIndex = name.lastIndexOf(".class");
                        String className = name.substring(beginIndex, endIndex);
                        handleClassesName.add(packageName + "." + className);
                    }
                }
            }
            // if not jar
            else {
                File dir = new File(resourcePath);
                File[] files = dir.listFiles();
                if (files == null) {
                    continue;
                }
                for (File file : files) {
                    String name = file.getName();
                    int endIndex = name.lastIndexOf(".class");
                    String className = name.substring(0, endIndex);
                    handleClassesName.add(packageName + "." + className);
                }
            }
        }
        return handleClassesName;
    }
}
