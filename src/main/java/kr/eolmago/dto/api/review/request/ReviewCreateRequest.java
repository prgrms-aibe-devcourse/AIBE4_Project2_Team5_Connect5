package kr.eolmago.dto.api.review.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "리뷰 생성 요청 DTO")
public record ReviewCreateRequest(

        @Schema(description = "거래 ID", example = "1")
        Long dealId,

        @Schema(description = "평점 (1~5)", example = "5")
        Integer rating,

        @Schema(description = "리뷰 내용", example = "거래가 매우 원활했습니다.")
        String content
) {
}
