package kr.eolmago.chat.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import kr.eolmago.domain.entity.auction.Auction;
import kr.eolmago.domain.entity.chat.ChatRoom;
import kr.eolmago.domain.entity.chat.ChatRoomType;
import kr.eolmago.domain.entity.user.User;
import kr.eolmago.repository.auction.AuctionRepository;
import kr.eolmago.repository.chat.ChatRoomRepository;
import kr.eolmago.repository.user.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;

final class ChatRoomServiceTestDoubles {

	final ChatRoomRepository chatRoomRepository;
	final AuctionRepository auctionRepository;
	final UserRepository userRepository;

	private final Map<UUID, Auction> auctionsById = new HashMap<>();
	private final Map<UUID, User> usersById = new HashMap<>();

	private final Map<Long, ChatRoom> roomsById = new HashMap<>();
	private final Map<UUID, ChatRoom> auctionRoomsByAuctionId = new HashMap<>();
	private final Map<UUID, ChatRoom> notificationRoomsByTargetUserId = new HashMap<>();

	private final AtomicLong roomIdSeq = new AtomicLong(1);

	private final Set<UUID> raceAuctionIds = new HashSet<>();
	private final Set<UUID> racedAlready = new HashSet<>();

	private ChatRoomServiceTestDoubles() {
		this.chatRoomRepository = mock(ChatRoomRepository.class);
		this.auctionRepository = mock(AuctionRepository.class);
		this.userRepository = mock(UserRepository.class);
		wire();
	}

	static ChatRoomServiceTestDoubles create() {
		return new ChatRoomServiceTestDoubles();
	}

	void putAuction(UUID auctionId, Auction auction) {
		auctionsById.put(auctionId, auction);
	}

	void putUser(UUID userId, User user) {
		usersById.put(userId, user);
	}

	void enableSaveRaceFor(UUID auctionId) {
		raceAuctionIds.add(auctionId);
	}

	ChatRoom seedAuctionRoom(Auction auction, User seller, User buyer) {
		ChatRoom room = ChatRoom.createAuctionRoom(auction, seller, buyer);
		saveInternal(room);
		return room;
	}

	ChatRoom findRoomOrThrow(Long roomId) {
		ChatRoom room = roomsById.get(roomId);
		if (room == null) throw new IllegalStateException("room not found in fake repo");
		return room;
	}

	private void wire() {
		when(auctionRepository.findById(any(UUID.class)))
			.thenAnswer(inv -> Optional.ofNullable(auctionsById.get(inv.getArgument(0))));

		when(userRepository.findById(any(UUID.class)))
			.thenAnswer(inv -> Optional.ofNullable(usersById.get(inv.getArgument(0))));

		when(chatRoomRepository.findById(anyLong()))
			.thenAnswer(inv -> Optional.ofNullable(roomsById.get(inv.getArgument(0))));

		when(chatRoomRepository.findByAuctionAuctionIdAndRoomType(any(UUID.class), any(ChatRoomType.class)))
			.thenAnswer(inv -> {
				UUID auctionId = inv.getArgument(0);
				ChatRoomType type = inv.getArgument(1);
				if (type != ChatRoomType.AUCTION) return Optional.empty();
				return Optional.ofNullable(auctionRoomsByAuctionId.get(auctionId));
			});

		when(chatRoomRepository.findByRoomTypeAndTargetUserId(any(ChatRoomType.class), any(UUID.class)))
			.thenAnswer(inv -> {
				ChatRoomType type = inv.getArgument(0);
				UUID targetUserId = inv.getArgument(1);
				if (type != ChatRoomType.NOTIFICATION) return Optional.empty();
				return Optional.ofNullable(notificationRoomsByTargetUserId.get(targetUserId));
			});

		when(chatRoomRepository.findMyRoomsByType(any(UUID.class), any(ChatRoomType.class)))
			.thenAnswer(inv -> {
				UUID userId = inv.getArgument(0);
				ChatRoomType type = inv.getArgument(1);

				List<ChatRoom> result = new ArrayList<>();
				for (ChatRoom room : roomsById.values()) {
					if (room.getRoomType() != type) continue;

					boolean isSeller = room.getSeller() != null && room.getSeller().getUserId().equals(userId);
					boolean isBuyer = room.getBuyer() != null && room.getBuyer().getUserId().equals(userId);
					boolean isTarget = room.getTargetUserId() != null && room.getTargetUserId().equals(userId);

					if (isSeller || isBuyer || isTarget) result.add(room);
				}
				return result;
			});

		when(chatRoomRepository.save(any(ChatRoom.class)))
			.thenAnswer(inv -> {
				ChatRoom room = inv.getArgument(0);
				UUID auctionId = extractAuctionId(room);

				// 경합 시뮬레이션: 첫 save는 예외를 던지되, "누군가 먼저 저장했다" 상태로 저장해 둠
				if (auctionId != null && raceAuctionIds.contains(auctionId) && racedAlready.add(auctionId)) {
					saveInternal(room);
					throw new DataIntegrityViolationException("race");
				}

				saveInternal(room);
				return room;
			});
	}

	private void saveInternal(ChatRoom room) {
		assignRoomIdIfMissing(room);
		roomsById.put(room.getChatRoomId(), room);

		if (room.getRoomType() == ChatRoomType.AUCTION) {
			UUID auctionId = extractAuctionId(room);
			if (auctionId != null) auctionRoomsByAuctionId.put(auctionId, room);
		}

		if (room.getRoomType() == ChatRoomType.NOTIFICATION) {
			UUID targetUserId = room.getTargetUserId();
			if (targetUserId != null) notificationRoomsByTargetUserId.put(targetUserId, room);
		}
	}

	private void assignRoomIdIfMissing(ChatRoom room) {
		Long id = room.getChatRoomId();
		if (id != null && id > 0) return;

		long newId = roomIdSeq.getAndIncrement();
		setField(room, "chatRoomId", newId);
	}

	private UUID extractAuctionId(ChatRoom room) {
		Object auctionObj = invokeGetter(room, "getAuction");
		if (auctionObj == null) auctionObj = getField(room, "auction");
		if (auctionObj instanceof Auction auction) return auction.getAuctionId();
		return null;
	}

	private static Object invokeGetter(Object target, String methodName) {
		try {
			Method m = target.getClass().getMethod(methodName);
			m.setAccessible(true);
			return m.invoke(target);
		} catch (Exception ignored) {
			return null;
		}
	}

	private static Object getField(Object target, String fieldName) {
		try {
			Field f = target.getClass().getDeclaredField(fieldName);
			f.setAccessible(true);
			return f.get(target);
		} catch (Exception ignored) {
			return null;
		}
	}

	private static void setField(Object target, String fieldName, Object value) {
		try {
			Field f = target.getClass().getDeclaredField(fieldName);
			f.setAccessible(true);
			f.set(target, value);
		} catch (Exception e) {
			throw new IllegalStateException("Failed to set field: " + fieldName, e);
		}
	}
}
