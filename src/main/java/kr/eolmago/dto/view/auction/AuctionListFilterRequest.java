package kr.eolmago.dto.view.auction;

import kr.eolmago.domain.entity.auction.enums.ItemCategory;

import java.util.List;

public record AuctionListFilterRequest(
        String keyword,
        ItemCategory category,
        List<String> brands,
        Integer minPrice,
        Integer maxPrice
) {
}