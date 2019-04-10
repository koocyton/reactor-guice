package com.doopp.kreactor.test.service;

import com.doopp.kreactor.test.entity.Coordinate;
import com.doopp.kreactor.test.entity.Point;
import reactor.core.publisher.Mono;

import java.util.List;

public interface MapApiService {

    String getAroundPointsApiUrl(String categoryName, String categoryId);

    Mono<List<Point>> searchPoints(String categoryName, String categoryId, Coordinate coordinate);
}
