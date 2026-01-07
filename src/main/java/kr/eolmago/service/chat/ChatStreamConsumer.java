package kr.eolmago.service.chat;

import static kr.eolmago.service.chat.ChatConstants.*;

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
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatStreamConsumer {

	private final StringRedisTemplate redisTemplate;
	private final ChatRoomRepository chatRoomRepository;
	private final ChatMessageRepository chatMessageRepository;
	private final UserRepository userRepository;
	private final SimpMessagingTemplate messagingTemplate;
	private final TransactionTemplate transactionTemplate;

	private final String consumerName = DEFAULT_CONSUMER_NAME;

	@Scheduled(fixedDelay = POLL_FIXED_DELAY_MS)
	public void poll() {
		List<MapRecord<String, Object, Object>> records;
		try {
			records = redisTemplate.opsForStream().read(
				Consumer.from(GROUP, consumerName),
				StreamReadOptions.empty()
					.count(POLL_COUNT)
					.block(Duration.ofSeconds(POLL_BLOCK_SECONDS)),
				StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed())
			);
		} catch (Exception e) {
			log.debug("chat stream read failed: {}", e.getMessage());
			return;
		}

		if (records == null || records.isEmpty()) return;

		for (MapRecord<String, Object, Object> record : records) {
			ProcessResult result = handleOne(record);

			if (result.shouldAck()) {
				redisTemplate.opsForStream().acknowledge(STREAM_KEY, GROUP, record.getId());
			}
			if (result.shouldPublish()) {
				messagingTemplate.convertAndSend(TOPIC_ROOM_PREFIX + result.roomId(), result.payload());
			}
		}
	}

	private ProcessResult handleOne(MapRecord<String, Object, Object> record) {
		Map<Object, Object> value = record.getValue();

		Long roomId;
		UUID senderId;
		String content;

		try {
			roomId = Long.valueOf(value.get(FIELD_ROOM_ID).toString());
			senderId = UUID.fromString(value.get(FIELD_SENDER_ID).toString());
			content = value.get(FIELD_CONTENT).toString();
		} catch (Exception e) {
			return ProcessResult.ackOnly();
		}

		if (!StringUtils.hasText(content)) return ProcessResult.ackOnly();

		ChatMessageResponse payload = transactionTemplate.execute(status -> {
			Optional<ChatRoom> roomOpt = chatRoomRepository.findById(roomId);
			if (roomOpt.isEmpty()) return null;

			Optional<User> senderOpt = userRepository.findById(senderId);
			if (senderOpt.isEmpty()) return null;

			ChatRoom room = roomOpt.get();
			User sender = senderOpt.get();

			boolean isSeller = room.getSeller().getUserId().equals(senderId);
			boolean isBuyer = room.getBuyer().getUserId().equals(senderId);
			if (!isSeller && !isBuyer) return null;

			ChatMessage saved = chatMessageRepository.save(ChatMessage.create(room, sender, content.trim()));

			room.updateLastMessageId(saved.getChatMessageId());
			chatRoomRepository.save(room);

			return ChatMessageResponse.from(saved);
		});

		if (payload == null) return ProcessResult.ackOnly();
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
