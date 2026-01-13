package com.booking.booking.repositories;

import java.time.Duration;
import java.util.Optional;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class IdempotencyRepository {

    private final StringRedisTemplate redis;

    public IdempotencyRepository(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public Optional<String> get(String key) {
        return Optional.ofNullable(
            redis.opsForValue().get("idempotency:" + key)
        );
    }

    public void save(String key, String json) {
        redis.opsForValue().set(
            "idempotency:" + key,
            json,
            Duration.ofHours(1)
        );
    }
}
