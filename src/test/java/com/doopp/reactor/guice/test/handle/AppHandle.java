package com.doopp.reactor.guice.test.handle;

import com.doopp.reactor.guice.annotation.Controller;
import com.doopp.reactor.guice.annotation.FileParam;
import com.doopp.reactor.guice.test.entity.User;
import com.doopp.reactor.guice.test.proto.hello.Hello;
import com.doopp.reactor.guice.view.ModelMap;
import com.doopp.reactor.guice.StatusMessageException;
import com.doopp.reactor.guice.test.entity.Point;
import com.doopp.reactor.guice.test.service.MapApiService;
import com.doopp.reactor.guice.test.service.TestService;
import com.google.inject.Inject;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@Path("/kreactor")
public class AppHandle {

    @Inject
    private TestService testService;

    @Inject
    private HttpClient httpClient;

    @Inject
    private MapApiService mapApiService;

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
        modelMap.addAttribute("id", id);
        return Mono.just("test");
    }

    @GET
    @Path("/test/image")
    @Produces({"image/jpeg"})
    public Mono<byte[]> testImage() {
        return httpClient
                .get()
                .uri("https://static.cnbetacdn.com/article/2019/0402/6398390c491f650.jpg")
                .responseContent()
                .aggregate()
                // .map(ByteBuf::retain)
                .map(byteBuf -> {
                    byte[] abc = new byte[byteBuf.retain().readableBytes()];
                    byteBuf.readBytes(abc);
                    return abc;
                });
    }

    @GET
    @Path("/test/points")
    @Produces(MediaType.APPLICATION_JSON)
    public Mono<List<Point>> testPoints() {
        return mapApiService
            .searchPoints("药房", "090601", null)
            .map(l->{
                System.out.println(l);
                return l;
            });
    }

    @POST
    @Path("/test/params")
    @Produces(MediaType.APPLICATION_JSON)
    public Mono<String[]> testParams(@FormParam("test") String[] abc) {
        return abc==null ? Mono.error(new StatusMessageException(500, "abc must init")) : Mono.just(abc);
    }

    @GET
    @Path("/test/redirect")
    @Produces(MediaType.TEXT_HTML)
    public Mono<String> testRedirect() {
        return Mono.just("redirect:/kreactor/test/json");
    }

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

    @POST
    @Path("/test/post-bean")
    public Mono<User> testPostBean(@BeanParam User user, @FileParam(value="image", path = "C:\\Users\\koocyton\\Desktop\\tmp") File[] file) {
        return Mono.just(user);
    }

    @POST
    @Path("/test/proto-post-bean")
    public Mono<Hello> testPostBean(@BeanParam Hello hello) {
        return Mono.just(hello);
    }
}
