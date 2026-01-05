package kr.eolmago.service.user;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RedisTemplate<String, String> redisTemplate;
    private final JwtService jwtService;

    private static final String PREFIX = "refresh:";

    public void save(UUID userId, String refreshToken) {
        redisTemplate.opsForValue().set(
                PREFIX + userId,
                refreshToken,
                jwtService.getRefreshTokenExpiryMillis(),
                TimeUnit.MILLISECONDS
        );
    }

    public String get(UUID userId) {
        return redisTemplate.opsForValue().get(PREFIX + userId);
    }

    public void delete(UUID userId) {
        redisTemplate.delete(PREFIX + userId);
    }

    public void update(UUID userId, String refreshToken) {
        delete(userId);
        save(userId, refreshToken);
    }

    public void rotate(UUID userId, String newToken) {
        delete(userId);
        save(userId, newToken);
    }

    public boolean validate(UUID userId, String refreshToken) {
        String stored = get(userId);
        return stored != null && stored.equals(refreshToken) && jwtService.validateToken(refreshToken);
    }
}
