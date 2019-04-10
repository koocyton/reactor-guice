package com.doopp.kreactor.test.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.LongSerializationPolicy;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.nio.charset.Charset;

public class HttpClientUtil {

    private static final HttpClient httpClient = HttpClient.create();

    private static final Gson gson = new GsonBuilder()
            .serializeNulls()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .setLongSerializationPolicy(LongSerializationPolicy.STRING)
            .create();

    public static Mono<String> get(String url) {
        return httpClient
                .get()
                .uri(url)
                .responseContent()
                .aggregate()
                .map(byteBuf -> byteBuf.toString(Charset.forName("UTF-8")));
    }

    public static <T> Mono<T> get(String url, Class<T> clazz) {
        return get(url).map(s->gson.fromJson(s, clazz));
    }
}
