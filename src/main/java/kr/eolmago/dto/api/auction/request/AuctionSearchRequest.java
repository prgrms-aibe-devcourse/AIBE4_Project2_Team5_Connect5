package kr.eolmago.dto.api.auction.request;

import kr.eolmago.domain.entity.auction.enums.AuctionStatus;
import kr.eolmago.domain.entity.auction.enums.ItemCategory;

import java.util.List;
import java.util.UUID;

public record AuctionSearchRequest(
        AuctionStatus status,
        String keyword,
        ItemCategory category,
        List<String> brands,
        Integer minPrice,
        Integer maxPrice,
        UUID userId
) {
}
