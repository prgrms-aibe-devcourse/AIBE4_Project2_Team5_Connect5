package kr.eolmago.dto.api.auction.request;

import jakarta.validation.constraints.*;

/**
 * 경매 수정 요청 DTO (DRAFT 상태에서만 수정 가능)
 */
public record AuctionUpdateRequest(
        @NotBlank(message = "제목을 입력해주세요.")
        @Size(min = 1, max = 100, message = "제목은 1~100자 이내로 입력해주세요.")
        String title,

        @Size(max = 5000, message = "설명은 5000자 이내로 입력해주세요.")
        String description,

        @NotNull(message = "시작가를 입력해주세요.")
        @Min(value = 10_000, message = "시작가는 10,000원 이상이어야 합니다.")
        @Max(value = 10_000_000, message = "시작가는 10,000,000원 이하여야 합니다.")
        Integer startPrice
) {
}
