package com.doopp.kreactor.test;

import com.doopp.kreactor.test.service.TestService;
import com.doopp.kreactor.test.service.impl.TestServiceImpl;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.LongSerializationPolicy;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import reactor.netty.http.client.HttpClient;

class Module extends AbstractModule {

    @Override
    public void configure() {
        bind(TestService.class).to(TestServiceImpl.class).in(Scopes.SINGLETON);
    }

    @Singleton
    @Provides
    public Gson gson () {
        return new GsonBuilder()
            .serializeNulls()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .setLongSerializationPolicy(LongSerializationPolicy.STRING)
            .create();
    }

    @Singleton
    @Provides
    public HttpClient httpClient () {
        return HttpClient.create();
    }
}