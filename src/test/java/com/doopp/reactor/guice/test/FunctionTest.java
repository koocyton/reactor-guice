package com.doopp.reactor.guice.test;

import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class FunctionTest {

    @Test
    public void testReadJarFile() throws IOException, URISyntaxException {

        String uri = "/com/doopp/reactor/guice";

        String p1 = "/Developer/Project/reactor-guice/target/classes";
        Path pp1 = Paths.get(p1 + uri);

        // jarPath : /Developer/Project/reactor-publisher/build/libs/reactor-publisher-1.0.jar
        // resourcePath : /com/doopp/gauss/app/handle
        Path p2 = Paths.get("/Developer/Project/reactor-publisher/build/libs/reactor-publisher-1.0.jar");
        FileSystem fs2 = FileSystems.newFileSystem(p2, null);
        Path pp2 = fs2.getPath("/com/doopp/gauss/app/handle");

        URL resource = getClass().getResource(uri);
        Path pp3 = Paths.get(resource.getPath());
        // Path p3 = Paths.get(resource.getPath());
        // FileSystem fs3 = FileSystems.newFileSystem(p3, null);
        // Path pp3 = fs3.getPath("/");

        Files.walkFileTree(pp2, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(java.nio.file.Path file, BasicFileAttributes attrs) throws IOException {
                String filePath = file.toUri().toString();
                if (filePath.endsWith(".class")) {
                    System.out.println(filePath);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
