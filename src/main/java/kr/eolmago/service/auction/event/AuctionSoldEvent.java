package kr.eolmago.service.auction.event;

import java.util.UUID;

public record AuctionSoldEvent(
	UUID auctionId,
	UUID sellerId,
	UUID buyerId
) {}
