package com.pfh.user.util;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RedisUtil {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    // =========== FAIL AUTHEN RATE LIMITING METHODS ============
    public boolean isRateLimited(String key, String userId, int maxAttempts) {
        ValueOperations<String, String> ops = redisTemplate.opsForValue();

        String countStr = ops.get(key);

        // If no record, create one and expiry
        int count = (countStr == null) ? 
                    0 : 
                    Integer.parseInt(countStr);

        return count >= maxAttempts;        
    }

    public void recordFailedAttempt(String key, String userId, Duration failWindowMs) {
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        // Current time in system's default zone
        
        Long count = ops.increment(key);
        
        if (count != null && count == 1L) {
            redisTemplate.expire(key, failWindowMs);
        }
    }

    public void resetAttempts(String key, String userId) {
        redisTemplate.delete(key);
    }
}
