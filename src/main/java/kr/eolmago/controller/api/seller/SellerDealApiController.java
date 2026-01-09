package kr.eolmago.controller.api.seller;

import kr.eolmago.domain.entity.deal.Deal;
import kr.eolmago.global.security.CustomUserDetails;
import kr.eolmago.repository.deal.DealRepository;
import kr.eolmago.repository.user.UserRepository;
import kr.eolmago.repository.auction.AuctionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DB 연결 테스트용 간단한 API 컨트롤러
 */
@RestController
@RequestMapping("/api/seller/deals")
@RequiredArgsConstructor
public class SellerDealApiController {

    private final DealRepository dealRepository;
    private final UserRepository userRepository;
    private final AuctionRepository auctionRepository;

    /**
     * DB 연결 테스트 - 모든 데이터 조회
     * GET /api/seller/deals
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getSellerDeals(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 1. 현재 로그인한 사용자 정보
            if (userDetails != null) {
                response.put("currentUser", Map.of(
                        "userId", userDetails.getUserId(),
                        "email", userDetails.getUsername()
                ));
            }

            // 2. Deal 테이블의 모든 데이터 (최대 10개)
            List<Deal> allDeals = dealRepository.findAll();
            response.put("totalDeals", allDeals.size());
            response.put("deals", allDeals.stream()
                    .limit(10)
                    .map(deal -> Map.of(
                            "dealId", deal.getDealId(),
                            "finalPrice", deal.getFinalPrice(),
                            "status", deal.getStatus().name(),
                            "createdAt", deal.getCreatedAt() != null ? deal.getCreatedAt().toString() : "N/A"
                    ))
                    .toList());

            // 3. User 테이블 카운트
            long userCount = userRepository.count();
            response.put("totalUsers", userCount);

            // 4. Auction 테이블 카운트
            long auctionCount = auctionRepository.count();
            response.put("totalAuctions", auctionCount);

            // 5. DB 연결 상태
            response.put("dbConnected", true);
            response.put("message", "DB 연결 성공! 데이터를 정상적으로 조회했습니다.");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("dbConnected", false);
            response.put("error", e.getMessage());
            response.put("message", "DB 연결 실패: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 특정 사용자의 거래만 조회 (테스트용)
     * GET /api/seller/deals/my
     */
    @GetMapping("/my")
    public ResponseEntity<Map<String, Object>> getMyDeals(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Map<String, Object> response = new HashMap<>();

        if (userDetails == null) {
            response.put("error", "로그인이 필요합니다.");
            return ResponseEntity.ok(response);
        }

        try {
            // 현재 사용자의 거래 조회
            List<Deal> myDeals = dealRepository.findBySeller_UserId(userDetails.getUserId());

            response.put("userId", userDetails.getUserId());
            response.put("totalMyDeals", myDeals.size());
            response.put("myDeals", myDeals.stream()
                    .map(deal -> Map.of(
                            "dealId", deal.getDealId(),
                            "finalPrice", deal.getFinalPrice(),
                            "status", deal.getStatus().name()
                    ))
                    .toList());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("error", e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * DB 연결 간단 테스트
     * GET /api/seller/deals/test
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> testConnection() {
        Map<String, Object> response = new HashMap<>();

        try {
            // 간단한 카운트 쿼리로 DB 연결 테스트
            long dealCount = dealRepository.count();
            long userCount = userRepository.count();
            long auctionCount = auctionRepository.count();

            response.put("success", true);
            response.put("message", "DB 연결 성공!");
            response.put("data", Map.of(
                    "deals", dealCount,
                    "users", userCount,
                    "auctions", auctionCount
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "DB 연결 실패: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());

            return ResponseEntity.ok(response);
        }
    }
}
