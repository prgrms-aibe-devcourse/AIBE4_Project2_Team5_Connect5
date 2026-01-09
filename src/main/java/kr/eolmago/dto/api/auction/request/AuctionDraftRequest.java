package kr.eolmago.dto.api.auction.request;

import jakarta.validation.constraints.*;
import kr.eolmago.domain.entity.auction.enums.ItemCategory;
import kr.eolmago.domain.entity.auction.enums.ItemCondition;

import java.util.List;
import java.util.Map;

public record AuctionDraftRequest(
        @NotBlank(message = "제목을 입력해주세요.")
        @Size(max = 100, message = "제목은 100자 이하여야 합니다.")
        String title,

        String description,

        @NotNull(message = "시작가를 입력해주세요.")
        @Min(value = 10_000, message = "시작가는 10,000원 이상이어야 합니다.")
        @Max(value = 10_000_000, message = "시작가는 10,000,000원 이하여야 합니다.")
        Integer startPrice,

        @NotNull(message = "경매 기간을 선택해주세요.")
        @Min(value = 12, message = "최소 12시간 이상이어야 합니다.")
        @Max(value = 168, message = "최대 7일 이하여야 합니다.")
        Integer durationHours,

        @NotBlank(message = "상품명을 입력해주세요.")
        @Size(min = 1, max = 100, message = "상품명은 1~100자 이내로 입력해주세요.")
        String itemName,

        @NotNull(message = "카테고리를 선택해주세요.")
        ItemCategory category,

        @NotNull(message = "상품 상태를 선택해주세요.")
        ItemCondition condition,

        Map<String, Object> specs,

        @NotNull(message = "이미지를 1장 이상 등록해주세요.")
        @Size(min = 1, message = "이미지를 1장 이상 등록해주세요.")
        List<@NotBlank String> imageUrls
) {
}
