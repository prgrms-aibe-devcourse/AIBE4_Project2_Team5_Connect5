package kr.eolmago.service.chat;

import static kr.eolmago.service.chat.ChatStreamPublisher.GROUP;
import static kr.eolmago.service.chat.ChatStreamPublisher.STREAM_KEY;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import kr.eolmago.domain.entity.chat.ChatMessage;
import kr.eolmago.domain.entity.chat.ChatRoom;
import kr.eolmago.domain.entity.user.User;
import kr.eolmago.dto.api.chat.response.ChatMessageResponse;
import kr.eolmago.repository.chat.ChatMessageRepository;
import kr.eolmago.repository.chat.ChatRoomRepository;
import kr.eolmago.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class ChatStreamConsumer {

	private final StringRedisTemplate redisTemplate;
	private final ChatRoomRepository chatRoomRepository;
	private final ChatMessageRepository chatMessageRepository;
	private final UserRepository userRepository;
	private final SimpMessagingTemplate messagingTemplate;
	private final TransactionTemplate transactionTemplate;

	private final String consumerName = "c1";

	@Scheduled(fixedDelay = 200)
	public void poll() {
		List<MapRecord<String, Object, Object>> records;

		try {
			records = redisTemplate.opsForStream().read(
				Consumer.from(GROUP, consumerName),
				StreamReadOptions.empty().count(50).block(Duration.ofSeconds(1)),
				StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed())
			);
		} catch (Exception e) {
			return;
		}

		if (records == null || records.isEmpty()) {
			return;
		}

		for (MapRecord<String, Object, Object> record : records) {
			ProcessResult result = handleOne(record);

			if (result.shouldAck()) {
				redisTemplate.opsForStream().acknowledge(STREAM_KEY, GROUP, record.getId());
			}

			if (result.shouldPublish()) {
				messagingTemplate.convertAndSend(
					"/topic/chat.rooms." + result.roomId(),
					result.payload()
				);
			}
		}
	}

	private ProcessResult handleOne(MapRecord<String, Object, Object> record) {
		Map<Object, Object> value = record.getValue();

		Long roomId;
		UUID senderId;
		String content;

		try {
			roomId = Long.valueOf(value.get("roomId").toString());
			senderId = UUID.fromString(value.get("senderId").toString());
			content = value.get("content").toString();
		} catch (Exception e) {
			return ProcessResult.ackOnly(); // 포맷 깨진 레코드는 버림
		}

		if (!StringUtils.hasText(content)) {
			return ProcessResult.ackOnly();
		}

		ChatMessageResponse payload = transactionTemplate.execute(status -> {
			Optional<ChatRoom> roomOpt = chatRoomRepository.findById(roomId);
			if (roomOpt.isEmpty()) {
				return null;
			}

			Optional<User> senderOpt = userRepository.findById(senderId);
			if (senderOpt.isEmpty()) {
				return null;
			}

			ChatRoom room = roomOpt.get();
			User sender = senderOpt.get();

			ChatMessage saved = chatMessageRepository.save(ChatMessage.create(room, sender, content.trim()));

			room.updateLastMessageId(saved.getChatMessageId());
			chatRoomRepository.save(room);

			return ChatMessageResponse.from(saved);
		});

		if (payload == null) {
			return ProcessResult.ackOnly(); // room/sender가 없으면 버림
		}

		// 여기까지 왔으면 "DB 커밋 완료 후"에만 실행되는 흐름이 됨
		return ProcessResult.ackAndPublish(roomId, payload);
	}

	private record ProcessResult(boolean shouldAck, boolean shouldPublish, Long roomId, ChatMessageResponse payload) {
		static ProcessResult ackOnly() {
			return new ProcessResult(true, false, null, null);
		}

		static ProcessResult ackAndPublish(Long roomId, ChatMessageResponse payload) {
			return new ProcessResult(true, true, roomId, payload);
		}
	}
}
