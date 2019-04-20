package com.doopp.reactor.guice.test.service;

import com.doopp.reactor.guice.test.entity.Coordinate;
import com.doopp.reactor.guice.test.entity.Point;
import reactor.core.publisher.Mono;

import java.util.List;

public interface MapApiService {

    String getAroundPointsApiUrl(String categoryName, String categoryId);

    Mono<List<Point>> searchPoints(String categoryName, String categoryId, Coordinate coordinate);
}
