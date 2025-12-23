package com.booking.booking.services;

import java.time.Duration;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.booking.booking.exceptions.ApiException;

@Service
public class LockService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    public boolean acquireLock(String key,Duration ttl){
        Boolean success = redisTemplate.
        opsForValue().
        setIfAbsent(key, "locked",ttl);
        return Boolean.TRUE.equals(success);    
    }

     public <T> T execute(String key, Supplier<T> action) {

        if (key == null || key.isBlank()) {
            return action.get();
        }

        Boolean acquired = redisTemplate
            .opsForValue()
            .setIfAbsent("idempotency:" + key, "LOCK", Duration.ofMinutes(10));

        if (Boolean.FALSE.equals(acquired)) {
            throw new ApiException(
                "Request already processed",
                HttpStatus.CONFLICT
            );
        }

        return action.get();
    }
    
    public void releaseLock(String key){
        redisTemplate.delete(key);
    }
}
