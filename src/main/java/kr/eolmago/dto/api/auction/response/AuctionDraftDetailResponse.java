package kr.eolmago.dto.api.auction.response;

import kr.eolmago.domain.entity.auction.enums.ItemCategory;
import kr.eolmago.domain.entity.auction.enums.ItemCondition;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record AuctionDraftDetailResponse(
        UUID auctionId,
        UUID sellerId,
        String title,
        String description,
        Integer startPrice,
        Integer durationHours,
        Integer bidIncrement,
        String itemName,
        ItemCategory category,
        ItemCondition condition,
        Map<String, Object> specs,
        List<String> imageUrls
) {
}