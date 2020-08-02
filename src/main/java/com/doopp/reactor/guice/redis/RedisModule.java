package com.doopp.reactor.guice.redis;

import com.google.inject.*;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import redis.clients.jedis.JedisPoolConfig;

public abstract class RedisModule extends AbstractModule {

    @Override
    protected final void configure() {
        this.initialize();
    }

    @Singleton
    @Inject
    private JedisPoolConfig jedisPoolConfig(@Named("redis.pool.maxTotal") int maxTotal,
                                            @Named("redis.pool.maxIdle") int maxIdle,
                                            @Named("redis.pool.minIdle") int minIdle,
                                            @Named("redis.pool.maxWaitMillis") int maxWaitMillis,
                                            @Named("redis.pool.lifo") boolean lifo,
                                            @Named("redis.pool.testOnBorrow") boolean testOnBorrow) {
        // Jedis池配置
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(maxTotal); // 最大分配的对象数
        config.setMaxIdle(maxIdle); // 最大能够保持idel状态的对象数
        config.setMinIdle(minIdle); // 最小空闲的对象数。2.5.1以上版本有效
        config.setMaxWaitMillis(maxWaitMillis); // 当池内没有返回对象时，最大等待时间
        config.setLifo(lifo); // 是否启用Lifo。如果不设置，默认为true。2.5.1以上版本有效
        config.setTestOnBorrow(testOnBorrow); // 当调用borrow Object方法时，是否进行有效性检查
        return config;
    }

    protected void bindShardedJedis(String name, String redisServers, JedisPoolConfig jedisPoolConfig) {
        this.bind(ShardedJedisHelper.class)
                .annotatedWith(Names.named(name))
                .toInstance(new ShardedJedisHelper(redisServers, jedisPoolConfigProvider.get()));
        // bind(DataSource.class).toProvider(dataSourceProviderType).in(Scopes.SINGLETON);
    }

    protected abstract void initialize();
}
