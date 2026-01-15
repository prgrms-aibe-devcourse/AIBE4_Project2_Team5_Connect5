package kr.eolmago.dto.api.auction.response;

public record SellerCredibilityResponse(
        String sellerAccount,
        String nickname,
        long completedDealCount,
        long reportCount
) {
}