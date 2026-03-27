package com.smartcanteen.backend.service.impl;

import com.smartcanteen.backend.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.PrivilegedAction;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistServiceImpl  implements TokenBlacklistService {

    private final RedisTemplate<String,Object> redisTemplate;
    private static final String BLACKLIST_PREFIX = "blacklist:";

    @Override
    public void blacklistToken(String token) {
        try{
            redisTemplate.opsForValue().set(
                    BLACKLIST_PREFIX+token,
                    "true",
                    1, TimeUnit.DAYS // token expiry time
            );
        }catch (Exception e){
            log.warn("Redis not available, skipping blacklist");
        }
    }

    @Override
    public boolean isBlacklisted(String token) {
        try{
            return redisTemplate.hasKey(BLACKLIST_PREFIX+token);
        }catch(Exception e){
            log.warn("Redis not available, skipping check");
            return  false;
        }
    }
}
