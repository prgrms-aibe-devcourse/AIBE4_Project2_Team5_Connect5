package kr.eolmago.controller.api.seller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.eolmago.dto.api.deal.response.SellerDealDetailResponse;
import kr.eolmago.dto.api.deal.response.SellerDealListResponse;
import kr.eolmago.global.security.CustomUserDetails;
import kr.eolmago.service.deal.SellerDealDetailService;
import kr.eolmago.service.deal.SellerDealService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 판매자 거래 관리 API 컨트롤러
 */
@Tag(name = "Seller Deal", description = "판매자 거래 관리 API")
@RestController
@RequestMapping("/api/seller/deals")
@RequiredArgsConstructor
public class SellerDealApiController {

    private final SellerDealService sellerDealService;
    private final SellerDealDetailService sellerDealDetailService;

    @Operation(summary = "판매자 거래 목록 조회")
    @GetMapping
    public ResponseEntity<SellerDealListResponse> getSellerDeals(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID sellerId = userDetails.getUserId();
        SellerDealListResponse response = sellerDealService.getSellerDeals(sellerId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "판매자 거래 상세 조회 (목록용)")
    @GetMapping("/list/{dealId}")
    public ResponseEntity<SellerDealListResponse.DealDto> getDealListDetail(
            @PathVariable Long dealId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID sellerId = userDetails.getUserId();
        SellerDealListResponse.DealDto response = sellerDealService.getDealDetail(dealId, sellerId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "판매자 거래 확정")
    @PostMapping("/{dealId}/confirm")
    public ResponseEntity<Void> confirmDeal(
            @PathVariable Long dealId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        sellerDealService.confirmDeal(dealId, userDetails.getUserId());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "판매자 거래 상세 조회 (상세 페이지용)")
    @GetMapping("/{dealId}")
    public ResponseEntity<SellerDealDetailResponse> getDealDetail(
            @PathVariable Long dealId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID sellerId = userDetails.getUserId();
        SellerDealDetailResponse response = sellerDealDetailService.getDealDetail(dealId, sellerId);
        return ResponseEntity.ok(response);
    }
}
