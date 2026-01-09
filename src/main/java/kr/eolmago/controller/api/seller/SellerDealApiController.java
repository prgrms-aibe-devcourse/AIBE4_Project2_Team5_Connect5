package kr.eolmago.controller.api.seller;

import kr.eolmago.domain.entity.deal.enums.DealStatus;
import kr.eolmago.dto.api.deal.SellerDealListDto;
import kr.eolmago.dto.view.deal.DealResponse;
import kr.eolmago.global.security.CustomUserDetails;
import kr.eolmago.service.deal.DealService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 판매자 거래 REST API 컨트롤러
 */
@RestController
@RequestMapping("/api/seller/deals")
@RequiredArgsConstructor
public class SellerDealApiController {

    private final DealService dealService;

    /**
     * 판매자의 모든 거래 조회 (상태별로 분류)
     * GET /api/seller/deals
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getSellerDeals(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "User not authenticated")
            );
        }

        // CustomUserDetails에서 userId 추출
        UUID sellerId = userDetails.getUserId();

        // 판매자의 모든 거래 조회
        List<DealResponse> allDeals = dealService.getDealsBySeller(sellerId);

        // 상태별로 분류
        List<SellerDealListDto> pending = allDeals.stream()
                .filter(deal -> "PENDING_CONFIRMATION".equals(deal.status()))
                .map(deal -> SellerDealListDto.from(deal, "구매자")) // TODO: 실제 구매자 이름 조회
                .collect(Collectors.toList());

        List<SellerDealListDto> ongoing = allDeals.stream()
                .filter(deal -> "CONFIRMED".equals(deal.status()))
                .map(deal -> SellerDealListDto.from(deal, "구매자"))
                .collect(Collectors.toList());

        List<SellerDealListDto> completed = allDeals.stream()
                .filter(deal -> "COMPLETED".equals(deal.status()))
                .map(deal -> SellerDealListDto.from(deal, "구매자"))
                .collect(Collectors.toList());

        List<SellerDealListDto> cancelled = allDeals.stream()
                .filter(deal -> "TERMINATED".equals(deal.status())
                        || "EXPIRED".equals(deal.status()))
                .map(deal -> SellerDealListDto.from(deal, "구매자"))
                .collect(Collectors.toList());

        // 응답 데이터 구성
        Map<String, Object> response = new HashMap<>();
        response.put("pending", pending);
        response.put("ongoing", ongoing);
        response.put("completed", completed);
        response.put("cancelled", cancelled);
        response.put("counts", Map.of(
                "pending", pending.size(),
                "ongoing", ongoing.size(),
                "completed", completed.size(),
                "cancelled", cancelled.size()
        ));

        return ResponseEntity.ok(response);
    }

    /**
     * 특정 상태의 거래만 조회
     * GET /api/seller/deals/status/{status}
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<SellerDealListDto>> getDealsByStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String status
    ) {
        if (userDetails == null) {
            return ResponseEntity.badRequest().build();
        }

        UUID sellerId = userDetails.getUserId();

        try {
            DealStatus dealStatus = DealStatus.valueOf(status);
            List<DealResponse> deals = dealService.getDealsBySellerAndStatus(sellerId, dealStatus);
            List<SellerDealListDto> response = deals.stream()
                    .map(deal -> SellerDealListDto.from(deal, "구매자"))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 거래 상세 정보 조회
     * GET /api/seller/deals/{dealId}
     */
    @GetMapping("/{dealId}")
    public ResponseEntity<DealResponse> getDealDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long dealId
    ) {
        if (userDetails == null) {
            return ResponseEntity.badRequest().build();
        }

        UUID sellerId = userDetails.getUserId();

        try {
            DealResponse deal = dealService.getDeal(dealId);

            // 판매자 본인의 거래인지 확인
            if (!deal.sellerId().equals(sellerId)) {
                return ResponseEntity.status(403).build(); // Forbidden
            }

            return ResponseEntity.ok(deal);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 판매자 거래 확정
     * POST /api/seller/deals/{dealId}/confirm
     */
    @PostMapping("/{dealId}/confirm")
    public ResponseEntity<DealResponse> confirmDeal(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long dealId
    ) {
        if (userDetails == null) {
            return ResponseEntity.badRequest().build();
        }

        UUID sellerId = userDetails.getUserId();

        try {
            DealResponse deal = dealService.getDeal(dealId);

            // 판매자 본인의 거래인지 확인
            if (!deal.sellerId().equals(sellerId)) {
                return ResponseEntity.status(403).build();
            }

            DealResponse confirmed = dealService.confirmBySeller(dealId);
            return ResponseEntity.ok(confirmed);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 거래 종료 (취소)
     * POST /api/seller/deals/{dealId}/terminate
     */
    @PostMapping("/{dealId}/terminate")
    public ResponseEntity<DealResponse> terminateDeal(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long dealId,
            @RequestBody Map<String, String> body
    ) {
        if (userDetails == null) {
            return ResponseEntity.badRequest().build();
        }

        UUID sellerId = userDetails.getUserId();

        try {
            DealResponse deal = dealService.getDeal(dealId);

            // 판매자 본인의 거래인지 확인
            if (!deal.sellerId().equals(sellerId)) {
                return ResponseEntity.status(403).build();
            }

            String reason = body.getOrDefault("reason", "판매자 취소");
            DealResponse terminated = dealService.terminateDeal(dealId, reason);
            return ResponseEntity.ok(terminated);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}