package kr.eolmago.service.chat;

import static kr.eolmago.service.chat.ChatConstants.*;

import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatStreamInitializer implements ApplicationRunner {

	private final StringRedisTemplate redisTemplate;

	@Override
	public void run(ApplicationArguments args) {
		try {
			redisTemplate.execute((RedisCallback<Object>) connection ->
				connection.execute(
					CMD_XGROUP,
					b(XGROUP_CREATE),
					b(STREAM_KEY),
					b(GROUP),
					b(XGROUP_START_ID),
					b(XGROUP_MKSTREAM)
				)
			);
			log.info("chat stream group ensured. key={}, group={}", STREAM_KEY, GROUP);
		} catch (Exception e) {
			log.info("chat stream group already exists(or cannot create): {}", e.getMessage());
		}
	}

	private static byte[] b(String v) {
		return v.getBytes(StandardCharsets.UTF_8);
	}
}
