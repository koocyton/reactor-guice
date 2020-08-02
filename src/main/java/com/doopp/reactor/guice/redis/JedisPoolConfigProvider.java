package com.doopp.reactor.guice.redis;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import redis.clients.jedis.JedisPoolConfig;

public class JedisPoolConfigProvider implements Provider<JedisPoolConfig> {

    private final JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();

    @Inject
    public void setMaxTotal(@Named("redis.pool.maxTotal") final int maxTotal) {
        jedisPoolConfig.setMaxTotal(maxTotal);
    }

    @Inject
    public void setMaxIdle(@Named("redis.pool.maxIdle") final int maxIdle) {
        jedisPoolConfig.setMaxIdle(maxIdle);
    }

    @Inject
    public void setMinIdle(@Named("redis.pool.minIdle") final int minIdle) {
        jedisPoolConfig.setMinIdle(minIdle);
    }

    @Inject
    public void setMaxWaitMillis(@Named("redis.pool.maxWaitMillis") final int maxWaitMillis) {
        jedisPoolConfig.setMaxWaitMillis(maxWaitMillis);
    }

    @Inject
    public void setLifo(@Named("redis.pool.lifo") final boolean lifo) {
        jedisPoolConfig.setLifo(lifo);
    }

    @Inject
    public void setTestOnBorrow(@Named("redis.pool.testOnBorrow") final boolean testOnBorrow) {
        jedisPoolConfig.setTestOnBorrow(testOnBorrow);
    }

    @Override
    public JedisPoolConfig get() {
        return jedisPoolConfig;
    }
}
