package kr.eolmago.service.auction.stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class BidResultStore {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public BidProcessingResult get(String key) {
        try {
            String raw = redisTemplate.opsForValue().get(key);
            if (raw == null) return null;
            return objectMapper.readValue(raw, BidProcessingResult.class);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean putPendingIfAbsent(String key, Duration ttl) {
        String json;
        try {
            json = objectMapper.writeValueAsString(BidProcessingResult.pending());
        } catch (JsonProcessingException e) {
            json = "{\"status\":\"PENDING\"}";
        }
        Boolean ok = redisTemplate.opsForValue().setIfAbsent(key, json, ttl);
        return Boolean.TRUE.equals(ok);
    }

    public void put(String key, BidProcessingResult value, Duration ttl) {
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, json, ttl);
        } catch (Exception e) {
            redisTemplate.opsForValue().set(key, "{\"status\":\"ERROR\",\"errorCode\":\"SYSTEM_ERROR\"}", ttl);
        }
    }
}