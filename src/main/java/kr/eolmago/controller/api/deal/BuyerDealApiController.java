package kr.eolmago.controller.api.deal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.eolmago.dto.api.deal.response.BuyerDealDetailResponse;
import kr.eolmago.dto.api.deal.response.BuyerDealListResponse;
import kr.eolmago.global.security.CustomUserDetails;
import kr.eolmago.service.deal.BuyerDealService;
import kr.eolmago.service.deal.DealPdfService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 구매자 거래 관리 API 컨트롤러
 */
@Tag(name = "Buyer Deal", description = "구매자 거래 관리 API")
@RestController
@RequestMapping("/api/buyer/deals")
@RequiredArgsConstructor
public class BuyerDealApiController {

    private final BuyerDealService buyerDealService;
    private final DealPdfService dealPdfService;

    @Operation(summary = "구매자 거래 목록 조회")
    @GetMapping
    public ResponseEntity<BuyerDealListResponse> getBuyerDeals(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID buyerId = userDetails.getUserId();
        BuyerDealListResponse response = buyerDealService.getBuyerDeals(buyerId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "구매자 거래 상세 조회 (목록용)")
    @GetMapping("/list/{dealId}")
    public ResponseEntity<BuyerDealListResponse.DealDto> getDealListDetail(
            @PathVariable Long dealId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID buyerId = userDetails.getUserId();
        BuyerDealListResponse.DealDto response = buyerDealService.getDealListDetail(dealId, buyerId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "구매자 거래 확정")
    @PostMapping("/{dealId}/confirm")
    public ResponseEntity<Void> confirmDeal(
            @PathVariable Long dealId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        buyerDealService.confirmDeal(dealId, userDetails.getUserId());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "구매자 수령 확인 (거래 완료 처리)")
    @PostMapping("/{dealId}/receive-confirm")
    public ResponseEntity<Void> receiveConfirm(
            @PathVariable Long dealId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        buyerDealService.receiveConfirm(dealId, userDetails.getUserId());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "구매자 거래 상세 조회 (상세 페이지용)")
    @GetMapping("/{dealId}")
    public ResponseEntity<BuyerDealDetailResponse> getDealDetail(
            @PathVariable Long dealId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID buyerId = userDetails.getUserId();
        BuyerDealDetailResponse response = buyerDealService.getDealDetail(dealId, buyerId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "구매자 거래확정서 PDF 다운로드", description = "완료된 거래(COMPLETED)의 거래확정서를 PDF로 다운로드합니다")
    @GetMapping("/{dealId}/pdf")
    public ResponseEntity<byte[]> downloadPdf(
            @PathVariable Long dealId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        byte[] pdfBytes = dealPdfService.generatePdfForBuyer(dealId, userDetails.getUserId());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "deal-confirmation-" + dealId + ".pdf");
        headers.setContentLength(pdfBytes.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }
}