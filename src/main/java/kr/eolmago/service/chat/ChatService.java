package kr.eolmago.service.chat;

import static kr.eolmago.service.chat.ChatConstants.MESSAGE_PAGE_SIZE;

import java.util.List;
import java.util.UUID;
import kr.eolmago.domain.entity.auction.Auction;
import kr.eolmago.domain.entity.chat.ChatMessage;
import kr.eolmago.domain.entity.chat.ChatRoom;
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

	@Transactional(readOnly = true)
	public List<ChatRoomSummaryResponse> getMyRooms(UUID userId) {
		return chatRoomRepository.findMyRooms(userId).stream()
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
		return chatRoomRepository.findByAuctionAuctionId(auctionId)
			.map(room -> {
				chatValidator.validateParticipant(room, requesterId);
				return room.getChatRoomId();
			})
			.orElseGet(() -> createRoomWithRaceHandling(auctionId, requesterId));
	}

	@Transactional(readOnly = true)
	public ChatRoom getRoomOrThrow(Long roomId) {
		return chatRoomRepository.findById(roomId)
			.orElseThrow(() -> new ChatException(ErrorCode.CHAT_ROOM_NOT_FOUND));
	}

	@Transactional(readOnly = true)
	public ChatRoom getRoomForUserOrThrow(UUID userId, Long roomId) {
		return findRoomAndValidateParticipant(roomId, userId);
	}

	public void publishMessage(Long roomId, UUID senderId, String content) {
		chatValidator.validateSendRequest(roomId, senderId, content);
		chatStreamPublisher.publish(roomId, senderId, content.trim());
	}

	private ChatRoom findRoomAndValidateParticipant(Long roomId, UUID userId) {
		ChatRoom room = chatRoomRepository.findById(roomId)
			.orElseThrow(() -> new ChatException(ErrorCode.CHAT_ROOM_NOT_FOUND));
		chatValidator.validateParticipant(room, userId);
		return room;
	}

	private Long createRoomWithRaceHandling(UUID auctionId, UUID requesterId) {
		Auction auction = auctionRepository.findById(auctionId)
			.orElseThrow(() -> new ChatException(ErrorCode.AUCTION_NOT_FOUND));

		User seller = auction.getSeller();
		User buyer = resolveBuyer(auction, requesterId);

		ChatRoom newRoom = ChatRoom.create(auction, seller, buyer);

		try {
			return chatRoomRepository.save(newRoom).getChatRoomId();
		} catch (DataIntegrityViolationException e) {
			ChatRoom room = chatRoomRepository.findByAuctionAuctionId(auctionId).orElseThrow(() -> e);
			chatValidator.validateParticipant(room, requesterId);
			return room.getChatRoomId();
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
}
