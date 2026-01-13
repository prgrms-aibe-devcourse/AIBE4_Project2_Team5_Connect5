package kr.eolmago.chat.service;

import static org.mockito.Mockito.*;

import java.util.UUID;

import kr.eolmago.domain.entity.auction.Auction;
import kr.eolmago.domain.entity.chat.ChatRoom;
import kr.eolmago.domain.entity.user.User;

final class ChatRoomScenario {

	private final ChatRoomServiceTestDoubles doubles;

	UUID auctionId;
	UUID sellerId;
	UUID buyerId;
	UUID otherId;

	Auction auction;
	User seller;
	User buyer;

	private Long existingRoomId;

	private ChatRoomScenario(ChatRoomServiceTestDoubles doubles) {
		this.doubles = doubles;
		this.auctionId = UUID.randomUUID();
		this.sellerId = UUID.randomUUID();
		this.buyerId = UUID.randomUUID();
		this.otherId = UUID.randomUUID();
	}

	static ChatRoomScenario given(ChatRoomServiceTestDoubles doubles) {
		return new ChatRoomScenario(doubles);
	}

	ChatRoomScenario auctionWithBuyer() {
		this.seller = user(sellerId);
		this.buyer = user(buyerId);

		this.auction = mock(Auction.class);
		when(auction.getAuctionId()).thenReturn(auctionId);
		when(auction.getSeller()).thenReturn(seller);
		when(auction.getBuyer()).thenReturn(buyer);

		doubles.putAuction(auctionId, auction);
		doubles.putUser(sellerId, seller);
		doubles.putUser(buyerId, buyer);
		doubles.putUser(otherId, user(otherId));
		return this;
	}

	ChatRoomScenario auctionBuyerNull() {
		this.seller = user(sellerId);

		this.auction = mock(Auction.class);
		when(auction.getAuctionId()).thenReturn(auctionId);
		when(auction.getSeller()).thenReturn(seller);
		when(auction.getBuyer()).thenReturn(null);

		doubles.putAuction(auctionId, auction);
		doubles.putUser(sellerId, seller);
		doubles.putUser(otherId, user(otherId));
		return this;
	}

	ChatRoomScenario roomAlreadyExists() {
		ChatRoom room = doubles.seedAuctionRoom(auction, seller, buyer);
		this.existingRoomId = room.getChatRoomId();
		return this;
	}

	ChatRoomScenario saveRaceOccurs() {
		doubles.enableSaveRaceFor(auctionId);
		return this;
	}

	Long existingRoomId() {
		return existingRoomId;
	}

	private static User user(UUID userId) {
		User u = mock(User.class);
		when(u.getUserId()).thenReturn(userId);
		return u;
	}
}
