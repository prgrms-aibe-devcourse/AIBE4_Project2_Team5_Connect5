package kr.eolmago.dto.api.deal.response;

import kr.eolmago.domain.entity.auction.enums.ItemCategory;
import kr.eolmago.domain.entity.auction.enums.ItemCondition;

import java.util.Map;
import java.util.UUID;

public record DealDetailDto(
        // 거래 기본 정보
        Long dealId,
        Long finalPrice,
        String status,
        String createdAt,
        Boolean sellerConfirmed,
        Boolean buyerConfirmed,
        String sellerConfirmedAt,
        String buyerConfirmedAt,
        String confirmedAt,
        String confirmByAt,
        String shipByAt,
        String completedAt,

        // 경매 정보
        UUID auctionId,
        String auctionTitle,

        // 상품 정보
        String itemName,
        ItemCategory category,       // 카테고리 enum
        ItemCondition condition,      // 상품 상태 enum
        Map<String, Object> specs,    // 스펙 정보 (JSONB)

        // 판매자 정보 / 구매자 정보
        UUID sellerId,
        UUID buyerId,
        String sellerNickname,
        String buyerNickname,

        // 이미지 URL
        String thumbnailUrl
) {
    /**
     * 카테고리 한글명 반환
     */
    public String getItemCategory() {
        return category != null ? category.getLabel() : null;
    }

    /**
     * 상품 상태 표시 (S급, A급 등)
     */
    public String getItemCondition() {
        return condition != null ? condition.name() + "급" : null;
    }
    /**
     * specs에서 브랜드 정보를 추출
     */
    public String getItemBrand() {
        if (specs == null) return null;
        Object brand = specs.get("brand");
        return brand != null ? brand.toString() : null;
    }

    /**
     * specs에서 용량 정보를 추출
     */
    public String getItemStorage() {
        if (specs == null) return null;
        Object storage = specs.get("storage");
        return storage != null ? storage.toString() : null;
    }
}
