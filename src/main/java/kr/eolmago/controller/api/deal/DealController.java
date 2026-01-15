package kr.eolmago.controller.api.deal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.eolmago.dto.api.deal.response.BuyerDealDetailResponse;
import kr.eolmago.dto.api.deal.response.BuyerDealListResponse;
import kr.eolmago.dto.api.deal.response.SellerDealDetailResponse;
import kr.eolmago.dto.api.deal.response.SellerDealListResponse;
import kr.eolmago.global.security.CustomUserDetails;
import kr.eolmago.service.deal.BuyerDealService;
import kr.eolmago.service.deal.DealPdfService;
import kr.eolmago.service.deal.SellerDealService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Deal", description = "거래 관련 API")
@RestController
@RequiredArgsConstructor
public class DealController {

    private final BuyerDealService buyerDealService;
    private final SellerDealService sellerDealService;
    private final DealPdfService dealPdfService;

    // =========================
    // Buyer: /api/buyer/deals
    // =========================

    @Operation(summary = "구매자 거래 목록 조회")
    @GetMapping("/api/buyer/deals")
    public ResponseEntity<BuyerDealListResponse> getBuyerDeals(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID buyerId = userDetails.getUserId();
        return ResponseEntity.ok(buyerDealService.getBuyerDeals(buyerId));
    }

    @Operation(summary = "구매자 거래 상세 조회 (목록용)")
    @GetMapping("/api/buyer/deals/list/{dealId}")
    public ResponseEntity<BuyerDealListResponse.DealDto> getBuyerDealListDetail(
            @PathVariable Long dealId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID buyerId = userDetails.getUserId();
        return ResponseEntity.ok(buyerDealService.getDealListDetail(dealId, buyerId));
    }

    @Operation(summary = "구매자 거래 확정")
    @PostMapping("/api/buyer/deals/{dealId}/confirm")
    public ResponseEntity<Void> buyerConfirmDeal(
            @PathVariable Long dealId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        buyerDealService.confirmDeal(dealId, userDetails.getUserId());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "구매자 수령 확인(거래 완료)")
    @PostMapping("/api/buyer/deals/{dealId}/receive-confirm")
    public ResponseEntity<Void> buyerReceiveConfirm(
            @PathVariable Long dealId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        buyerDealService.receiveConfirm(dealId, userDetails.getUserId());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "구매자 거래 상세 조회 (상세 페이지용)")
    @GetMapping("/api/buyer/deals/{dealId}")
    public ResponseEntity<BuyerDealDetailResponse> getBuyerDealDetail(
            @PathVariable Long dealId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID buyerId = userDetails.getUserId();
        return ResponseEntity.ok(buyerDealService.getDealDetail(dealId, buyerId));
    }

    @Operation(summary = "구매자 거래확정서 PDF 다운로드", description = "완료된 거래(COMPLETED)의 거래확정서를 PDF로 다운로드합니다")
    @GetMapping("/api/buyer/deals/{dealId}/pdf")
    public ResponseEntity<byte[]> downloadBuyerPdf(
            @PathVariable Long dealId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        byte[] pdfBytes = dealPdfService.generatePdfForBuyer(dealId, userDetails.getUserId());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "deal-confirmation-" + dealId + ".pdf");
        headers.setContentLength(pdfBytes.length);

        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }

    // =========================
    // Seller: /api/seller/deals
    // =========================

    @Operation(summary = "판매자 거래 목록 조회")
    @GetMapping("/api/seller/deals")
    public ResponseEntity<SellerDealListResponse> getSellerDeals(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID sellerId = userDetails.getUserId();
        return ResponseEntity.ok(sellerDealService.getSellerDeals(sellerId));
    }

    @Operation(summary = "판매자 거래 상세 조회 (목록용)")
    @GetMapping("/api/seller/deals/list/{dealId}")
    public ResponseEntity<SellerDealListResponse.DealDto> getSellerDealListDetail(
            @PathVariable Long dealId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID sellerId = userDetails.getUserId();
        return ResponseEntity.ok(sellerDealService.getDealListDetail(dealId, sellerId));
    }

    @Operation(summary = "판매자 거래 확정")
    @PostMapping("/api/seller/deals/{dealId}/confirm")
    public ResponseEntity<Void> sellerConfirmDeal(
            @PathVariable Long dealId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        sellerDealService.confirmDeal(dealId, userDetails.getUserId());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "판매자 거래 상세 조회 (상세 페이지용)")
    @GetMapping("/api/seller/deals/{dealId}")
    public ResponseEntity<SellerDealDetailResponse> getSellerDealDetail(
            @PathVariable Long dealId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID sellerId = userDetails.getUserId();
        return ResponseEntity.ok(sellerDealService.getDealDetail(dealId, sellerId));
    }

    @Operation(summary = "판매자 거래확정서 PDF 다운로드", description = "확정 이후(CONFIRMED, COMPLETED) 거래의 거래확정서를 PDF로 다운로드합니다")
    @GetMapping("/api/seller/deals/{dealId}/pdf")
    public ResponseEntity<byte[]> downloadSellerPdf(
            @PathVariable Long dealId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        byte[] pdfBytes = dealPdfService.generatePdfForSeller(dealId, userDetails.getUserId());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "deal-confirmation-" + dealId + ".pdf");
        headers.setContentLength(pdfBytes.length);

        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }
}
