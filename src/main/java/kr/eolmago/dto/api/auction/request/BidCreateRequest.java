package kr.eolmago.dto.api.auction.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BidCreateRequest(
        int amount,

        @NotBlank(message = "요청 식별값은 필수입니다.")
        @Size(min = 8, max = 64, message = "요청 식별값은 8~64자여야 합니다.")
        String clientRequestId
) {
}
