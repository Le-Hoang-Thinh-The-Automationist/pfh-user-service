package com.pfh.user.service;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RedisService    {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    // =========== FAIL AUTHEN RATE LIMITING METHODS ============
    public boolean isRateLimited(String key, String id, int maxAttempts) {
        ValueOperations<String, String> ops = redisTemplate.opsForValue();

        String countStr = ops.get(key + id);

        // If no record, create one and expiry
        int count = (countStr == null) ? 
                    0 : 
                    Integer.parseInt(countStr);

        return count >= maxAttempts;        
    }

    public void recordFailedAttempt(String key, String id, Duration failWindowMs) {
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        // Current time in system's default zone
        
        Long count = ops.increment(key + id);
        
        if (count != null && count == 1L) {
            redisTemplate.expire(key + id, failWindowMs);
        }
    }

    public void resetAttempts(String key, String id) {
        redisTemplate.delete(key + id);
    }
}
