package kr.eolmago.service.chat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatStreamPublisher {

	public static final String STREAM_KEY = "chat:messages";
	public static final String GROUP = "chat-group";

	private final StringRedisTemplate redisTemplate;

	public RecordId publish(Long roomId, UUID senderId, String content) {
		Map<String, String> fields = new HashMap<>();
		fields.put("roomId", String.valueOf(roomId));
		fields.put("senderId", senderId.toString());
		fields.put("content", content);

		RecordId recordId = redisTemplate.opsForStream().add(STREAM_KEY, fields);

		// 그룹 없으면 생성 (stream 생성 이후에만 가능)
		try {
			redisTemplate.opsForStream().createGroup(STREAM_KEY, GROUP);
		} catch (Exception ignored) {
			// BUSYGROUP 등은 무시
		}

		return recordId;
	}
}
