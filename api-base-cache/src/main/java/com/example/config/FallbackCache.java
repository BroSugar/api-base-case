package com.example.config;

import org.springframework.cache.Cache;
import org.springframework.cache.support.NullValue;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class FallbackCache implements Cache {
    private final Cache primary;   // Redis
    private final Cache fallback;  // Caffeine

    public FallbackCache(
            Cache primary,
            Cache fallback
    ) {
        this.primary = primary;
        this.fallback = fallback;
    }

    @Override
    public String getName() {
        return primary.getName();
    }

    @Override
    public Object getNativeCache() {
        return primary.getNativeCache();
    }

    @Override
    public ValueWrapper get(Object key) {
        try {
            ValueWrapper value = primary.get(key);
            if (value != null && !(value.get() instanceof NullValue)) {
                fallback.put(key, value.get()); // 同步到本地
                return value;
            }
        } catch (Exception e) {
            if (isRedisException(e)) {
                System.err.println("⚠️ [FALLBACK] Redis 不可用，降级读取本地缓存 key: " + key + " | 原因: " + e.getMessage());
                return fallback.get(key);
            } else {
                throw e;
            }
        }
        return fallback.get(key);
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        return null;
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        try {
            T value = primary.get(key, valueLoader);
            if (value != null && !(value instanceof NullValue)) {
                fallback.put(key, value);
            }
            return value;
        } catch (Exception e) {
            if (isRedisException(e)) {
                System.err.println("⚠️ [FALLBACK] Redis 不可用，降级加载本地缓存 key: " + key + " | 原因: " + e.getMessage());
                return fallback.get(key, valueLoader);
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public CompletableFuture<?> retrieve(Object key) {
        return Cache.super.retrieve(key);
    }

    @Override
    public <T> CompletableFuture<T> retrieve(Object key, Supplier<CompletableFuture<T>> valueLoader) {
        return Cache.super.retrieve(key, valueLoader);
    }

    @Override
    public void put(Object key, Object value) {
        try {
            primary.put(key, value);
        } catch (Exception e) {
            if (isRedisException(e)) {
                System.err.println("⚠️ [FALLBACK] Redis 不可用，跳过写入 key: " + key);
                // 可选：fallback.put(key, value);
            } else {
                throw e;
            }
        }
        // 无论如何写入本地（可选，推荐关闭避免不一致）
        // fallback.put(key, value);
    }

    @Override
    public ValueWrapper putIfAbsent(Object key, Object value) {
        try {
            return primary.putIfAbsent(key, value);
        } catch (Exception e) {
            if (isRedisException(e)) {
                System.err.println("⚠️ [FALLBACK] Redis 不可用，降级本地 putIfAbsent key: " + key);
                return fallback.putIfAbsent(key, value);
            } else {
                throw e;
            }
        }
    }

    @Override
    public void evict(Object key) {
        try {
            primary.evict(key);
        } catch (Exception e) {
            if (isRedisException(e)) {
                System.err.println("⚠️ [FALLBACK] Redis 不可用，降级本地 evict key: " + key);
            }
        }
        fallback.evict(key); // 本地必须清理
    }

    @Override
    public boolean evictIfPresent(Object key) {
        return Cache.super.evictIfPresent(key);
    }

    @Override
    public void clear() {
        try {
            primary.clear();
        } catch (Exception e) {
            if (isRedisException(e)) {
                System.err.println("⚠️ [FALLBACK] Redis 不可用，降级本地 clear");
            }
        }
        fallback.clear();
    }

    @Override
    public boolean invalidate() {
        return Cache.super.invalidate();
    }

    private boolean isRedisException(Throwable e) {
        return e instanceof RedisConnectionFailureException ||
                e instanceof RedisSystemException ||
                e instanceof DataAccessException;
    }
}
