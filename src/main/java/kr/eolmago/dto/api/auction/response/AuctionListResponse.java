package kr.eolmago.dto.api.auction.response;

import kr.eolmago.domain.entity.auction.Auction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 경매 목록 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuctionListResponse {

    private UUID auctionId;
    private String title;
    private String thumbnailUrl;  // 대표 이미지

    // 가격 정보
    private Integer currentPrice;
    private Integer startPrice;

    // 입찰 정보
    private Integer bidCount;
    private Integer viewCount;

    // 일정
    private OffsetDateTime endAt;
    private String remainingTime;  // "2일 5시간 남음"

    // 상태
    private String status;

    // Entity → DTO 변환
    public static AuctionListResponse from(Auction auction) {
        return AuctionListResponse.builder()
                .auctionId(auction.getAuctionId())
                .title(auction.getTitle())
                .thumbnailUrl(getThumbnailUrl(auction))  // 헬퍼 메서드
                .currentPrice(auction.getCurrentPrice())
                .startPrice(auction.getStartPrice())
                .bidCount(auction.getBidCount())
                .viewCount(auction.getViewCount())
                .endAt(auction.getEndAt())
                .remainingTime(calculateRemainingTime(auction.getEndAt()))
                .status(auction.getStatus().name())
                .build();
    }

    // 썸네일 이미지 추출 (나중에 auction_images 조인 시 구현)
    private static String getThumbnailUrl(Auction auction) {
        // TODO: auction_images에서 is_thumbnail=true인 이미지 가져오기
        return "/images/default-auction.jpg";
    }

    // 남은 시간 계산
    private static String calculateRemainingTime(OffsetDateTime endAt) {
        if (endAt == null) return "";

        OffsetDateTime now = OffsetDateTime.now();
        if (endAt.isBefore(now)) {
            return "종료됨";
        }

        long days = java.time.Duration.between(now, endAt).toDays();
        long hours = java.time.Duration.between(now, endAt).toHours() % 24;

        if (days > 0) {
            return String.format("%d일 %d시간 남음", days, hours);
        } else if (hours > 0) {
            return String.format("%d시간 남음", hours);
        } else {
            long minutes = java.time.Duration.between(now, endAt).toMinutes();
            return String.format("%d분 남음", minutes);
        }
    }

}