package kr.eolmago.dto.api.deal.response;

import kr.eolmago.domain.entity.deal.enums.DealStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

// 거래확정서 PDF 생성용 DTO
public record DealPdfDto(
    Long dealId,
    UUID auctionId,
    String auctionTitle,
    String itemName,
    UUID sellerId,
    String sellerEmail,
    String sellerPhoneNumber,
    UUID buyerId,
    String buyerEmail,
    String buyerPhoneNumber,
    Long finalPrice,
    DealStatus status,
    OffsetDateTime completedAt,
    String shippingNumber,
    String shippingCarrierCode,
    OffsetDateTime createdAt
) {
}
