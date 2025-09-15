package com.pfh.user.util;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import com.pfh.user.config.AppConstant;
import com.pfh.user.config.LoginRateLimitingProperties;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RedisUtil {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private final LoginRateLimitingProperties loginRateLimitingProperties;

    private static final String PREFIX_IP = "login:ip:";
    private static final String PREFIX_USER = "login:user:";

    // =========== FAIL AUTHEN RATE LIMITING METHODS ============
    public boolean isAuthenUserRateLimited(String userId, int maxAttempts) {
        String key = PREFIX_USER + userId;
        ValueOperations<String, String> ops = redisTemplate.opsForValue();

        String countStr = ops.get(key);

        // If no record, create one and expiry
        int count = (countStr == null) ? 
                    0 : 
                    Integer.parseInt(countStr);

        return count >= maxAttempts;        
    }

    public void recordAuthenUserFailedAttempt(String userId, Duration failWindowMs) {
        String key = PREFIX_USER + userId;
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        // Current time in system's default zone
        
        Long count = ops.increment(key);
        
        if (count != null && count == 1L) {
            redisTemplate.expire(key, failWindowMs);
        }
        
    }

    public void resetAuthenUserFailedAttempts(String userId) {
        String key = PREFIX_USER + userId;
        redisTemplate.delete(key);
    }

    // =========== IP RATE LIMITING METHODS ============
    public boolean isIpRateLimited(String ip) {
        String key = PREFIX_IP + ip;
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        String countStr = ops.get(key);
        
        // If no record, create one and expiry
        int count = (countStr == null) ? 
                    0 : 
                    Integer.parseInt(countStr);
        
        return count >= AppConstant.MAX_FAILED_IP_LOGIN_ATTEMPTS;
    }

    public void recordIpFailedAttempt(String ip) {
        String key = PREFIX_IP + ip;

        ValueOperations<String, String> ops = redisTemplate.opsForValue();

        Long expireMs = loginRateLimitingProperties.getIpAttemptWindowMs();

        // Use Redis atomic increment
        Long count = ops.increment(key);

        // Set expiry only if key is new
        if (count != null && count == 1L) {
            redisTemplate.expire(key, Duration.ofMillis(expireMs));
        }
    }

    public void resetIpAttempts(String ip) {
        String key = PREFIX_IP + ip;
        redisTemplate.delete(key);
    }

}
