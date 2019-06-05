package com.doopp.reactor.guice.test;

import io.netty.channel.ChannelOption;
import io.netty.handler.codec.http.HttpHeaderNames;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class FunctionTest {

    public void testStaticJarFile() {
        DisposableServer disposableServer = HttpServer.create()
            .tcpConfiguration(tcpServer ->
                tcpServer.option(ChannelOption.SO_KEEPALIVE, true)
            )
            .route(routes->{
                routes.route(req->true, (req, resp) -> {
                    try {
                        URI resourceUri = new URI("jar", "file:/D:/project/reactor-publisher/build/libs/reactor-publisher-1.0.jar!/public" + req.uri(), null);
                        //System.out.println(resourceUri.toURL());
                        //System.out.println(resourceUri.getPath());
                        //System.out.println(resourceUri.getScheme());
                        //System.out.println(resourceUri.toURL().getPath());
                        String[] jarPathInfo = resourceUri.toURL().getPath().split("!");
                        System.out.println("\n>>>");
                        System.out.println("1" + jarPathInfo[0]);
                        System.out.println("2" + jarPathInfo[1]);
                        if (jarPathInfo[0].startsWith("file:")) {
                            jarPathInfo[0] = java.io.File.separator.equals("\\")
                                ? jarPathInfo[0].substring(6)
                                : jarPathInfo[0].substring(5);
                        }
                        System.out.println("3" + jarPathInfo[0]);
                        java.nio.file.Path jarPath = Paths.get(jarPathInfo[0]);
                        System.out.println("4" + jarPath);
                        FileSystem fs = FileSystems.newFileSystem(jarPath, null);
                        java.nio.file.Path resourcePath = fs.getPath(jarPathInfo[1]);
                        System.out.println("5" + resourcePath);

                        if (Files.isDirectory(resourcePath)) {
                            System.out.println("6 isDIR");
                            return resp.sendRedirect(req.uri() + "/");
                        }
                        System.out.println("7" + resourcePath);
                        resp.header(HttpHeaderNames.CONTENT_LENGTH, String.valueOf(Files.size(resourcePath)));
                        return resp.sendFile(resourcePath).then();
                    }
                    catch(Exception e) {
                        e.printStackTrace();
                        return null;
                    }

                });
            })
            .host("127.0.0.1")
            .port(8083)
            .wiretap(true)
            .bindNow();

        System.out.printf("\n>>> KReactor Server Running http://%s:%d/ ... \n\n", "127.0.0.1", 8083);

        disposableServer.onDispose().block();
    }


    public void testReadJarFile() throws IOException, URISyntaxException {

        //String uri = "/com/doopp/reactor/guice";

        //String p1 = "/Developer/Project/reactor-guice/target/classes";
        //Path pp1 = Paths.get(p1 + uri);

        // jarPath : /Developer/Project/reactor-publisher/build/libs/reactor-publisher-1.0.jar
        // resourcePath : /com/doopp/gauss/app/handle
        Path p2 = Paths.get("D:/project/reactor-publisher/build/libs/reactor-publisher-1.0.jar");
        FileSystem fs2 = FileSystems.newFileSystem(p2, null);
        Path pp2 = fs2.getPath("/com/doopp/gauss");

        //URL resource = getClass().getResource(uri);
        //Path pp3 = Paths.get(resource.getPath());
        // Path p3 = Paths.get(resource.getPath());
        // FileSystem fs3 = FileSystems.newFileSystem(p3, null);
        // Path pp3 = fs3.getPath("/");

        URI kkURI = new URI("jar", "file:/D:/project/reactor-publisher/build/libs/reactor-publisher-1.0.jar!/com/doopp/gauss", null);
        // FileSystemProvider.installedProviders();
        // pp2 = Paths.get(new URI("jar", "file:/D:/project/reactor-publisher/build/libs/reactor-publisher-1.0.jar!/com/doopp/gauss", null));
        System.out.println(kkURI);
        System.out.println(kkURI);
        Files.walkFileTree(Paths.get(kkURI.toURL().getPath()), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(java.nio.file.Path file, BasicFileAttributes attrs) throws IOException {
                String filePath = file.toUri().toString();
                // String filePath = file.toUri().getPath();
                System.out.println(file.toAbsolutePath().toString());
                if (filePath.endsWith(".class")) {
                    // System.out.println(filePath);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
