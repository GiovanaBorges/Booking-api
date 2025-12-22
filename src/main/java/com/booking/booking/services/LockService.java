package com.booking.booking.services;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

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
    
    public void releaseLock(String key){
        redisTemplate.delete(key);
    }
}
