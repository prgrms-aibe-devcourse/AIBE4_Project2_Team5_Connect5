package kr.eolmago.service.chat;

import static kr.eolmago.service.chat.ChatConstants.MESSAGE_PAGE_SIZE;

import java.util.List;
import java.util.UUID;

import kr.eolmago.domain.entity.auction.Auction;
import kr.eolmago.domain.entity.chat.ChatMessage;
import kr.eolmago.domain.entity.chat.ChatRoom;
import kr.eolmago.domain.entity.chat.ChatRoomType;
import kr.eolmago.domain.entity.user.User;
import kr.eolmago.dto.api.chat.response.ChatMessageResponse;
import kr.eolmago.dto.api.chat.response.ChatRoomSummaryResponse;
import kr.eolmago.global.exception.ErrorCode;
import kr.eolmago.repository.auction.AuctionRepository;
import kr.eolmago.repository.chat.ChatMessageRepository;
import kr.eolmago.repository.chat.ChatRoomRepository;
import kr.eolmago.repository.user.UserRepository;
import kr.eolmago.service.chat.exception.ChatException;
import kr.eolmago.service.chat.validation.ChatValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatService {

	private final ChatRoomRepository chatRoomRepository;
	private final ChatMessageRepository chatMessageRepository;
	private final AuctionRepository auctionRepository;
	private final UserRepository userRepository;

	private final ChatStreamPublisher chatStreamPublisher;
	private final ChatValidator chatValidator;
	private final ChatSystemUserProvider systemUserProvider;

	@Transactional(readOnly = true)
	public List<ChatRoomSummaryResponse> getMyRooms(UUID userId, ChatRoomType roomType) {
		return chatRoomRepository.findMyRoomsByType(userId, roomType).stream()
			.map(room -> ChatRoomSummaryResponse.from(room, userId))
			.toList();
	}

	@Transactional(readOnly = true)
	public List<ChatMessageResponse> getMessages(UUID userId, Long roomId, Long cursor) {
		findRoomAndValidateParticipant(roomId, userId);

		Pageable pageable = PageRequest.of(0, MESSAGE_PAGE_SIZE);

		List<ChatMessage> page = (cursor == null)
			? chatMessageRepository.findByChatRoomChatRoomIdOrderByChatMessageIdDesc(roomId, pageable)
			: chatMessageRepository.findByChatRoomChatRoomIdAndChatMessageIdLessThanOrderByChatMessageIdDesc(roomId, cursor, pageable);

		return page.stream().map(ChatMessageResponse::from).toList();
	}

	@Transactional
	public Long createOrGetRoom(UUID auctionId, UUID requesterId) {
		return chatRoomRepository.findByAuctionAuctionIdAndRoomType(auctionId, ChatRoomType.AUCTION)
			.map(room -> {
				chatValidator.validateParticipant(room, requesterId);
				return room.getChatRoomId();
			})
			.orElseGet(() -> createAuctionRoomWithRaceHandling(auctionId, requesterId));
	}

	@Transactional
	public Long getOrCreateNotificationRoom(UUID userId) {
		return chatRoomRepository.findByRoomTypeAndTargetUserId(ChatRoomType.NOTIFICATION, userId)
			.map(ChatRoom::getChatRoomId)
			.orElseGet(() -> createNotificationRoom(userId));
	}

	public void publishMessage(Long roomId, UUID senderId, String content) {
		ChatRoom room = chatRoomRepository.findById(roomId)
			.orElseThrow(() -> new ChatException(ErrorCode.CHAT_ROOM_NOT_FOUND));

		if (room.getRoomType() == ChatRoomType.NOTIFICATION) {
			UUID botId = systemUserProvider.notificationBotUserId();
			if (!senderId.equals(botId)) {
				throw new ChatException(ErrorCode.CHAT_NOTIFICATION_READ_ONLY);
			}
		}

		chatValidator.validateSendRequest(roomId, senderId, content);
		chatStreamPublisher.publish(roomId, senderId, content.trim());
	}


	public void publishNotificationMessage(UUID receiverUserId, String content) {
		Long roomId = getOrCreateNotificationRoom(receiverUserId);
		UUID botId = systemUserProvider.notificationBotUserId();
		publishMessage(roomId, botId, content);
	}

	@Transactional(readOnly = true)
	public ChatRoom getRoomOrThrow(Long roomId) {
		return chatRoomRepository.findById(roomId)
			.orElseThrow(() -> new ChatException(ErrorCode.CHAT_ROOM_NOT_FOUND));
	}

	private ChatRoom findRoomAndValidateParticipant(Long roomId, UUID userId) {
		ChatRoom room = chatRoomRepository.findById(roomId)
			.orElseThrow(() -> new ChatException(ErrorCode.CHAT_ROOM_NOT_FOUND));
		chatValidator.validateParticipant(room, userId);
		return room;
	}

	private Long createAuctionRoomWithRaceHandling(UUID auctionId, UUID requesterId) {
		Auction auction = auctionRepository.findById(auctionId)
			.orElseThrow(() -> new ChatException(ErrorCode.AUCTION_NOT_FOUND));

		User seller = auction.getSeller();
		User buyer = auction.getBuyer();

		if (buyer == null) {
			throw new ChatException(ErrorCode.CHAT_ROOM_NOT_AVAILABLE_YET);
		}

		boolean isSeller = seller.getUserId().equals(requesterId);
		boolean isBuyer = buyer.getUserId().equals(requesterId);
		if (!isSeller && !isBuyer) {
			throw new ChatException(ErrorCode.CHAT_FORBIDDEN_AUCTION);
		}

		ChatRoom newRoom = ChatRoom.createAuctionRoom(auction, seller, buyer);

		try {
			return chatRoomRepository.save(newRoom).getChatRoomId();
		} catch (DataIntegrityViolationException e) {
			ChatRoom room = chatRoomRepository
				.findByAuctionAuctionIdAndRoomType(auctionId, ChatRoomType.AUCTION)
				.orElseThrow(() -> e);
			chatValidator.validateParticipant(room, requesterId);
			return room.getChatRoomId();
		}
	}


	private Long createNotificationRoom(UUID userId) {
		UUID botId = systemUserProvider.notificationBotUserId();

		User botUser = userRepository.findById(botId)
			.orElseThrow(() -> new ChatException(ErrorCode.USER_NOT_FOUND));

		User targetUser = userRepository.findById(userId)
			.orElseThrow(() -> new ChatException(ErrorCode.USER_NOT_FOUND));

		ChatRoom room = ChatRoom.createNotificationRoom(botUser, targetUser);

		try {
			return chatRoomRepository.save(room).getChatRoomId();
		} catch (DataIntegrityViolationException e) {
			ChatRoom existing = chatRoomRepository
				.findByRoomTypeAndTargetUserId(ChatRoomType.NOTIFICATION, userId)
				.orElseThrow(() -> e);
			return existing.getChatRoomId();
		}
	}

	private User resolveBuyer(Auction auction, UUID requesterId) {
		User seller = auction.getSeller();
		User buyer = auction.getBuyer();

		if (buyer == null) {
			if (seller.getUserId().equals(requesterId)) {
				throw new ChatException(ErrorCode.CHAT_SELLER_CANNOT_CREATE_AS_BUYER);
			}
			return userRepository.findById(requesterId)
				.orElseThrow(() -> new ChatException(ErrorCode.USER_NOT_FOUND));
		}

		boolean isSeller = seller.getUserId().equals(requesterId);
		boolean isBuyer = buyer.getUserId().equals(requesterId);
		if (!isSeller && !isBuyer) {
			throw new ChatException(ErrorCode.CHAT_FORBIDDEN_AUCTION);
		}

		return buyer;
	}

	@Transactional(readOnly = true)
	public ChatRoom getRoomForUserOrThrow(UUID userId, Long roomId) {
		ChatRoom room = chatRoomRepository.findById(roomId)
			.orElseThrow(() -> new ChatException(ErrorCode.CHAT_ROOM_NOT_FOUND));

		chatValidator.validateParticipant(room, userId);
		return room;
	}
}
