package kr.eolmago.service.chat;

import static kr.eolmago.service.chat.ChatConstants.*;

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

	private final StringRedisTemplate redisTemplate;

	public RecordId publish(Long roomId, UUID senderId, String content) {
		Map<String, String> fields = new HashMap<>();
		fields.put(FIELD_ROOM_ID, String.valueOf(roomId));
		fields.put(FIELD_SENDER_ID, senderId.toString());
		fields.put(FIELD_CONTENT, content);

		return redisTemplate.opsForStream().add(STREAM_KEY, fields);
	}
}
