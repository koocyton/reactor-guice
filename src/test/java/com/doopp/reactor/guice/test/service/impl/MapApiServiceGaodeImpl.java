package com.doopp.reactor.guice.test.service.impl;

import com.doopp.reactor.guice.annotation.Service;
import com.doopp.reactor.guice.test.entity.Coordinate;
import com.doopp.reactor.guice.test.entity.Point;
import com.doopp.reactor.guice.test.entity.PointsResponse;
import com.doopp.reactor.guice.test.service.MapApiService;
import com.doopp.reactor.guice.test.util.HttpClientUtil;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.hash.Hashing;
import com.google.common.net.UrlEscapers;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MapApiServiceGaodeImpl implements MapApiService {

    private static final String gaodeSecretKey = "...";

    private static final String gaodeApiUrl = "https://restapi.amap.com/v3/place/around";

    @Override
    public String getAroundPointsApiUrl(String categoryName, String categoryId) {

        Map<String, String> params = new HashMap<>();
        params.put("key", UrlEscapers.urlFormParameterEscaper().escape(gaodeSecretKey));
        params.put("location", "116.434585,39.943567");
        // params.put("location", coordinate.getLatitude()+";"+coordinate.getLongitude());
        params.put("keywords", UrlEscapers.urlFormParameterEscaper().escape(categoryName));
        params.put("types", categoryId);
        params.put("city", UrlEscapers.urlFormParameterEscaper().escape("北京"));
        params.put("radius", "1000");
        params.put("sortrule", "distance");
        params.put("offset", "25");
        params.put("page", "1");
        params.put("output", "JSON");

        Map<String, String> paramsSorted = ImmutableSortedMap
                .<String,String>naturalOrder()
                .putAll(params)
                .build();

        String queryParams = Joiner.on("&").withKeyValueSeparator("=").join(paramsSorted);

        // String md5Sig = Hashing.md5().hashBytes((queryParams + gaodeSecretKey).getBytes()).toString();
        String md5Sig = Hashing.goodFastHash(256).hashBytes((queryParams + gaodeSecretKey).getBytes()).toString();

        return gaodeApiUrl + "?" + queryParams + "&sig=" + md5Sig;
    }

    @Override
    public Mono<List<Point>> searchPoints(String categoryName, String categoryId, Coordinate coordinate) {
        String requestUrl = getAroundPointsApiUrl(categoryName, categoryId);
        return HttpClientUtil
                .get(requestUrl, PointsResponse.class)
                .map(PointsResponse::getPois);
    }
}
