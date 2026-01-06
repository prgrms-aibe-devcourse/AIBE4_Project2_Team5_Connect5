package kr.eolmago.chat.service;

import java.util.UUID;

import kr.eolmago.domain.entity.auction.Auction;
import kr.eolmago.domain.entity.chat.ChatRoom;
import kr.eolmago.domain.entity.user.User;

final class ChatRoomScenario {

	private final ChatRoomServiceTestDoubles doubles;

	final UUID auctionId = UUID.randomUUID();
	final UUID sellerId = UUID.randomUUID();
	final UUID buyerId = UUID.randomUUID();
	final UUID requesterId = UUID.randomUUID();
	final UUID otherId = UUID.randomUUID();

	final User seller = ChatRoomFixtures.user(sellerId);
	final User buyer = ChatRoomFixtures.user(buyerId);
	final User requester = ChatRoomFixtures.user(requesterId);

	final Auction auctionWithBuyer = ChatRoomFixtures.auction(auctionId, seller, buyer);
	final Auction auctionBuyerNull = ChatRoomFixtures.auction(auctionId, seller, null);

	private Long existingRoomId;

	private ChatRoomScenario(ChatRoomServiceTestDoubles doubles) {
		this.doubles = doubles;
	}

	static ChatRoomScenario given(ChatRoomServiceTestDoubles doubles) {
		return new ChatRoomScenario(doubles);
	}

	ChatRoomScenario auctionWithBuyer() {
		doubles.putAuction(auctionId, auctionWithBuyer);
		return this;
	}

	ChatRoomScenario auctionBuyerNull() {
		doubles.putAuction(auctionId, auctionBuyerNull);
		return this;
	}

	ChatRoomScenario requesterUserExists() {
		doubles.putUser(requesterId, requester);
		return this;
	}

	ChatRoomScenario roomAlreadyExists() {
		ChatRoom existing = doubles.seedRoom(auctionWithBuyer, seller, buyer);
		this.existingRoomId = existing.getChatRoomId();
		return this;
	}

	ChatRoomScenario saveRaceOccurs() {
		doubles.enableSaveRaceFor(auctionId);
		return this;
	}

	Long existingRoomId() {
		if (existingRoomId == null) throw new IllegalStateException("existingRoomId not set");
		return existingRoomId;
	}
}
