package kr.eolmago.controller.api.buyer;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.eolmago.dto.api.deal.response.BuyerDealDetailResponse;
import kr.eolmago.dto.api.deal.response.BuyerDealListResponse;
import kr.eolmago.global.security.CustomUserDetails;
import kr.eolmago.service.deal.BuyerDealDetailService;
import kr.eolmago.service.deal.BuyerDealService;
import lombok.RequiredArgsConstructor;
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
    private final BuyerDealDetailService buyerDealDetailService;

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
        BuyerDealListResponse.DealDto response = buyerDealService.getDealDetail(dealId, buyerId);
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


    @Operation(summary = "구매자 거래 상세 조회 (상세 페이지용)")
    @GetMapping("/{dealId}")
    public ResponseEntity<BuyerDealDetailResponse> getDealDetail(
            @PathVariable Long dealId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID buyerId = userDetails.getUserId();
        BuyerDealDetailResponse response = buyerDealDetailService.getDealDetail(dealId, buyerId);
        return ResponseEntity.ok(response);
    }
}
