package kr.eolmago.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 설정
 *
 * 역할:
 * - Redis 연결 설정
 * - RedisTemplate 빈 생성
 *
 *  * 참고:
 *  * - 팀원 누구나 RedisTemplate 주입받아서 사용 가능
 */
@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    @Value("${spring.data.redis.password:}") // 기본값 빈 문자열 (로컬용)
    private String password;

    @Value("${spring.data.redis.ssl.enabled:false}") // 기본값 false
    private boolean sslEnabled;

    /**
     * Redis 연결 팩토리
     *  - Redis 서버와의 연결 관리
     *  - 모든 RedisTemplate이 이 연결 사용
     *
     * @return RedisConnectionFactory
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(host);
        config.setPort(port);
        
        if (password != null && !password.isBlank()) {
            config.setPassword(password);
        }

        // SSL 설정 적용
        if (sslEnabled) {
            LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                    .useSsl()
                    .build();
            return new LettuceConnectionFactory(config, clientConfig);
        }

        return new LettuceConnectionFactory(config);
    }

    /**
     * RedisTemplate<String,String> 빈 생성
     *  - Key-Value 모두 문자열인 경우 사용
     *
     * @return RedisTemplate<String, String>
     */
    @Bean
    public RedisTemplate<String, String> redisTemplate(
            RedisConnectionFactory connectionFactory
    ) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // String Serializer 사용 (String → byte[])
        StringRedisSerializer serializer = new StringRedisSerializer();

        // Key, Value 모두 String으로 직렬화
        template.setKeySerializer(serializer);
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(serializer);
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * RedisTemplate<String, Object> 빈 생성
     *  - Key는 문자열, Value는 객체인 경우 사용
     *  - BidRedisService, RedisLockService 등에서 사용
     *
     * @return RedisTemplate<String, Object>
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplateObject(
            RedisConnectionFactory connectionFactory
    ) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key는 String, Value는 JSON으로 직렬화
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer valueSerializer = new GenericJackson2JsonRedisSerializer();

        template.setKeySerializer(keySerializer);
        template.setValueSerializer(valueSerializer);
        template.setHashKeySerializer(keySerializer);
        template.setHashValueSerializer(valueSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
