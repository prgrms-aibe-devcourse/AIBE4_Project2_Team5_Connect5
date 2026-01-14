package kr.eolmago.service.auction.stream;

import kr.eolmago.dto.api.auction.response.BidCreateResponse;

public record BidProcessingResult(
        String status,
        BidCreateResponse response,
        String errorCode,
        String errorMessage
) {
    public static BidProcessingResult pending() {
        return new BidProcessingResult("PENDING", null, null, null);
    }

    public static BidProcessingResult success(BidCreateResponse resp) {
        return new BidProcessingResult("SUCCESS", resp, null, null);
    }

    public static BidProcessingResult error(String code, String message) {
        return new BidProcessingResult("ERROR", null, code, message);
    }

    public boolean isPending() { return "PENDING".equals(status); }
    public boolean isSuccess() { return "SUCCESS".equals(status); }
    public boolean isError() { return "ERROR".equals(status); }
}