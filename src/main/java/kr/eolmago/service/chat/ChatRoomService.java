package kr.eolmago.service.chat;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import kr.eolmago.domain.entity.auction.Auction;
import kr.eolmago.domain.entity.chat.ChatRoom;
import kr.eolmago.domain.entity.user.User;
import kr.eolmago.dto.api.chat.response.ChatRoomSummaryResponse;
import kr.eolmago.repository.auction.AuctionRepository;
import kr.eolmago.repository.chat.ChatRoomRepository;
import kr.eolmago.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

	private final ChatRoomRepository chatRoomRepository;
	private final AuctionRepository auctionRepository;
	private final UserRepository userRepository;

	/**
	 * 테스트/개발용: buyer가 없어도(=LIVE) 요청자를 buyer로 간주해 방 생성
	 * - 단, seller는 buyer로 방 생성 못하게 막음(원하면 풀어도 됨)
	 * - 기존 방이 있으면 참여자만 roomId 반환
	 */
	@Transactional
	public Long createOrGetRoom(UUID auctionId, UUID requesterId) {
		Optional<ChatRoom> existing = chatRoomRepository.findByAuctionAuctionId(auctionId);
		if (existing.isPresent()) {
			ChatRoom room = existing.get();
			validateParticipant(room, requesterId);
			return room.getChatRoomId();
		}

		Auction auction = auctionRepository.findById(auctionId)
			.orElseThrow(() -> new IllegalArgumentException("auction not found"));

		User seller = auction.getSeller();
		User buyer = auction.getBuyer();

		if (buyer == null) {
			if (seller.getUserId().equals(requesterId)) {
				throw new IllegalStateException("seller cannot create room as buyer");
			}
			buyer = userRepository.findById(requesterId)
				.orElseThrow(() -> new IllegalArgumentException("user not found"));
		} else {
			boolean isSeller = seller.getUserId().equals(requesterId);
			boolean isBuyer = buyer.getUserId().equals(requesterId);
			if (!isSeller && !isBuyer) {
				throw new IllegalStateException("no permission for this auction chat");
			}
		}

		ChatRoom newRoom = ChatRoom.create(auction, seller, buyer);

		try {
			ChatRoom saved = chatRoomRepository.save(newRoom);
			return saved.getChatRoomId();
		} catch (DataIntegrityViolationException e) {
			// 경합으로 누군가 먼저 만들었을 수 있음 → 다시 조회
			ChatRoom room = chatRoomRepository.findByAuctionAuctionId(auctionId)
				.orElseThrow(() -> e);
			validateParticipant(room, requesterId);
			return room.getChatRoomId();
		}
	}

	@Transactional(readOnly = true)
	public List<ChatRoomSummaryResponse> getMyRooms(UUID userId) {
		List<ChatRoom> rooms = chatRoomRepository.findMyRooms(userId);
		return rooms.stream()
			.map(room -> ChatRoomSummaryResponse.from(room, userId))
			.collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public ChatRoom getRoomOrThrow(Long roomId) {
		return chatRoomRepository.findById(roomId)
			.orElseThrow(() -> new IllegalArgumentException("chat room not found"));
	}

	public void validateParticipant(ChatRoom room, UUID userId) {
		boolean isSeller = room.getSeller().getUserId().equals(userId);
		boolean isBuyer = room.getBuyer().getUserId().equals(userId);
		if (!isSeller && !isBuyer) {
			throw new IllegalStateException("no permission for this chat room");
		}
	}
}
