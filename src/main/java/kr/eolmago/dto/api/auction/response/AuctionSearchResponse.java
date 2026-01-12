package kr.eolmago.dto.api.auction.response;

import kr.eolmago.domain.entity.auction.enums.AuctionStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AuctionSearchResponse(
        UUID auctionId,         // 경매 ID
        Long auctionItemId,     // 경매 아이템 ID
        String title,           // 경매 제목
        String thumbnailUrl,    // 썸네일 이미지 URL
        String sellerNickname,  // 판매자 닉네임
        Integer currentPrice,   // 현재 가격 (입찰가)
        int bidCount,           // 입찰 횟수
        int favoriteCount,      // 찜 개수
        OffsetDateTime endAt,   // 경매 종료 시간
        String remainingTime,   // 남은 시간 (포맷팅)
        AuctionStatus status    // 경매 상태
) {
}
