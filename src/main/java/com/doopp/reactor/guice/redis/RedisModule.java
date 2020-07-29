package com.doopp.reactor.guice.redis;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.name.Names;
import org.apache.ibatis.mapping.Environment;
import org.mybatis.guice.environment.EnvironmentProvider;
import redis.clients.jedis.JedisPoolConfig;

public abstract class RedisModule extends AbstractModule {

    @Override
    protected final void configure() {
        this.initialize();
    }

    protected void bindInstance(String name, String redisServices, JedisPoolConfig jedisPoolConfig) {
        this.bind(ShardedJedisHelper.class)
                .annotatedWith(Names.named(name))
                .toInstance(new ShardedJedisHelper(redisServices, jedisPoolConfig));
    }

    protected abstract void initialize();
}
