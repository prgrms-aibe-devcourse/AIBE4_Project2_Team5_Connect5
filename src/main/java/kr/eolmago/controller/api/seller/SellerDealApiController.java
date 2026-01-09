package kr.eolmago.controller.api.seller;

import kr.eolmago.domain.entity.deal.Deal;
import kr.eolmago.global.security.CustomUserDetails;
import kr.eolmago.repository.deal.DealRepository;
import kr.eolmago.repository.user.UserRepository;
import kr.eolmago.repository.auction.AuctionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 판매자 거래 관리 API 컨트롤러
 * JWT 인증 기반 - 로그인한 판매자의 거래만 반환
 */
@Slf4j
@RestController
@RequestMapping("/api/seller/deals")
@RequiredArgsConstructor
public class SellerDealApiController {

    private final DealRepository dealRepository;
    private final UserRepository userRepository;
    private final AuctionRepository auctionRepository;

    /**
     * 로그인한 판매자의 모든 거래 조회
     * GET /api/seller/deals
     *
     * JWT 토큰에서 사용자 정보를 추출하여 해당 판매자의 거래만 반환
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getSellerDeals(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 로그인 확인
            if (userDetails == null) {
                log.warn("인증되지 않은 요청");
                response.put("error", "로그인이 필요합니다.");
                return ResponseEntity.status(401).body(response);
            }

            // JWT에서 사용자 ID 추출
            UUID sellerId = userDetails.getUserId();
            log.info("판매자 거래 조회 요청 - sellerId: {}", sellerId);

            // ⭐ 핵심: 현재 로그인한 사용자가 판매자인 거래만 조회
            List<Deal> myDeals = dealRepository.findBySeller_UserId(sellerId);
            log.info("조회된 거래 수: {}", myDeals.size());

            // 현재 사용자 정보
            response.put("currentUser", Map.of(
                    "userId", sellerId,
                    "email", userDetails.getEmail()
            ));

            // 내 거래 데이터
            response.put("totalDeals", myDeals.size());
            response.put("deals", myDeals.stream()
                    .map(deal -> Map.of(
                            "dealId", deal.getDealId(),
                            "finalPrice", deal.getFinalPrice(),
                            "status", deal.getStatus().name(),
                            "createdAt", deal.getCreatedAt() != null ? deal.getCreatedAt().toString() : "N/A"
                    ))
                    .toList());

            // 통계 정보 (전체)
            long totalUsers = userRepository.count();
            long totalAuctions = auctionRepository.count();

            response.put("totalUsers", totalUsers);
            response.put("totalAuctions", totalAuctions);
            response.put("dbConnected", true);
            response.put("message", "내 거래 데이터를 조회했습니다.");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("거래 조회 중 오류 발생", e);
            response.put("dbConnected", false);
            response.put("error", e.getMessage());
            response.put("message", "데이터 조회 실패: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 특정 거래 상세 조회 (권한 검증)
     * GET /api/seller/deals/{dealId}
     */
    @GetMapping("/{dealId}")
    public ResponseEntity<Map<String, Object>> getDealDetail(
            @PathVariable Long dealId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (userDetails == null) {
                response.put("error", "로그인이 필요합니다.");
                return ResponseEntity.status(401).body(response);
            }

            UUID sellerId = userDetails.getUserId();
            log.info("거래 상세 조회 - dealId: {}, sellerId: {}", dealId, sellerId);

            // 거래 조회
            Deal deal = dealRepository.findById(dealId)
                    .orElseThrow(() -> new RuntimeException("거래를 찾을 수 없습니다."));

            // 권한 확인: 내가 판매자인 거래인지 검증
            if (!deal.getSeller().getUserId().equals(sellerId)) {
                log.warn("권한 없는 거래 접근 시도 - dealId: {}, userId: {}", dealId, sellerId);
                response.put("error", "접근 권한이 없습니다.");
                return ResponseEntity.status(403).body(response);
            }

            // 거래 상세 정보
            response.put("deal", Map.of(
                    "dealId", deal.getDealId(),
                    "finalPrice", deal.getFinalPrice(),
                    "status", deal.getStatus().name(),
                    "createdAt", deal.getCreatedAt().toString()
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("거래 상세 조회 실패 - dealId: {}", dealId, e);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * DB 연결 테스트
     * GET /api/seller/deals/test
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> testConnection() {
        Map<String, Object> response = new HashMap<>();

        try {
            long dealCount = dealRepository.count();
            long userCount = userRepository.count();
            long auctionCount = auctionRepository.count();

            response.put("success", true);
            response.put("message", "DB 연결 성공!");
            response.put("data", Map.of(
                    "totalDeals", dealCount,
                    "totalUsers", userCount,
                    "totalAuctions", auctionCount
            ));

            log.info("DB 연결 테스트 성공 - deals: {}, users: {}, auctions: {}",
                    dealCount, userCount, auctionCount);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("DB 연결 테스트 실패", e);
            response.put("success", false);
            response.put("message", "DB 연결 실패: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());

            return ResponseEntity.status(500).body(response);
        }
    }
}
