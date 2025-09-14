package com.example.config;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
public class FallbackCompositeCacheManager implements CacheManager {
    private final RedisCacheManager redisCacheManager;
    private final CaffeineCacheManager caffeineCacheManager;

    public FallbackCompositeCacheManager(
            RedisCacheManager redisCacheManager,
            CaffeineCacheManager caffeineCacheManager
    ) {
        this.redisCacheManager = redisCacheManager;
        this.caffeineCacheManager = caffeineCacheManager;
    }

    @Override
    public Cache getCache(String name) {
        return new FallbackCache(redisCacheManager.getCache(name), caffeineCacheManager.getCache(name));
    }

    @Override
    public Collection<String> getCacheNames() {
        return redisCacheManager.getCacheNames();
    }
}
