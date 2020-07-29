package com.doopp.reactor.guice.redis;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import org.apache.ibatis.mapping.Environment;
import org.mybatis.guice.environment.EnvironmentProvider;

public abstract class RedisModule extends AbstractModule {

    @Override
    protected final void configure() {
        this.initialize();
        this.bind(Environment.class).toProvider(EnvironmentProvider.class).in(Scopes.SINGLETON);
    }

    protected abstract void initialize();
}
