package kr.eolmago.chat.service;

import static org.mockito.Mockito.*;

import java.util.UUID;

import kr.eolmago.domain.entity.auction.Auction;
import kr.eolmago.domain.entity.user.User;

final class ChatRoomFixtures {

	private ChatRoomFixtures() {}

	static User user(UUID userId) {
		User user = mock(User.class);
		when(user.getUserId()).thenReturn(userId);
		return user;
	}

	static Auction auction(UUID auctionId, User seller, User buyerOrNull) {
		Auction auction = mock(Auction.class);
		when(auction.getAuctionId()).thenReturn(auctionId);
		when(auction.getSeller()).thenReturn(seller);
		when(auction.getBuyer()).thenReturn(buyerOrNull);
		return auction;
	}
}
