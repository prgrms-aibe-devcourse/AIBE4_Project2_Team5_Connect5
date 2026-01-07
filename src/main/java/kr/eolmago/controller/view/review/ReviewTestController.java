package kr.eolmago.controller.view.review;

import kr.eolmago.domain.entity.deal.Deal;
import kr.eolmago.domain.entity.deal.enums.DealStatus;
import kr.eolmago.dto.view.review.ReviewResponse;
import kr.eolmago.repository.deal.DealRepository;
import kr.eolmago.service.review.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Review 테스트 컨트롤러
 * 
 * 경로: /test/review
 * 용도: Review 기능 테스트를 위한 웹 UI
 */
@Slf4j
@Controller
@RequestMapping("/test/review")
@RequiredArgsConstructor
public class ReviewTestController {

    private final ReviewService reviewService;
    private final DealRepository dealRepository;

    /**
     * 리뷰 목록 페이지
     */
    @GetMapping
    public String list(Model model) {
        List<ReviewResponse> reviews = reviewService.getAllReviews();
        model.addAttribute("reviews", reviews);
        log.info("📋 리뷰 목록 조회: {} 개", reviews.size());
        return "pages/review/review-list";
    }

    /**
     * 리뷰 상세 페이지
     */
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        ReviewResponse review = reviewService.getReview(id);
        model.addAttribute("review", review);
        log.info("🔍 리뷰 상세 조회: ID={}", id);
        return "pages/review/review-detail";
    }

    /**
     * 리뷰 작성 폼
     * 
     * 수정: 판매자/구매자 선택 제거
     * Deal 선택만으로 판매자/구매자 자동 결정
     * COMPLETED 상태의 거래만 제공
     */
    @GetMapping("/create")
    public String createForm(Model model) {
        // 방법 1: Repository 메서드 사용 (DealRepository에 메서드가 있다면)
        // List<Deal> deals = dealRepository.findByStatus(DealStatus.COMPLETED);
        
        // 방법 2: Stream filter 사용 (즉시 적용 가능)
        List<Deal> deals = dealRepository.findAll().stream()
                .filter(deal -> deal.getStatus() == DealStatus.COMPLETED)
                .toList();
        
        model.addAttribute("deals", deals);
        
        log.info("✏️ 리뷰 작성 폼: {} 개의 완료된 거래 제공 (COMPLETED만)", deals.size());
        return "pages/review/review-create";
    }

    /**
     * 리뷰 작성 처리
     * 
     * 수정: Deal에서 판매자/구매자 자동 추출
     * 비즈니스 검증: COMPLETED 상태 확인
     */
    @PostMapping
    public String create(
            @RequestParam Long dealId,
            @RequestParam int rating,
            @RequestParam String content) {
        
        // Deal 조회
        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new IllegalArgumentException("거래를 찾을 수 없습니다: " + dealId));
        
        // 비즈니스 검증: COMPLETED 상태 확인
        if (deal.getStatus() != DealStatus.COMPLETED) {
            log.warn("⚠️ 리뷰 작성 실패: 거래가 완료 상태가 아님 (Deal={}, Status={})", dealId, deal.getStatus());
            throw new IllegalStateException("완료된 거래만 리뷰를 작성할 수 있습니다");
        }
        
        // Deal에서 판매자/구매자 자동 추출
        var sellerId = deal.getSeller().getUserId();
        var buyerId = deal.getBuyer().getUserId();
        
        log.info("📝 리뷰 작성: Deal={}, Seller={}, Buyer={}, Rating={}", 
                dealId, sellerId, buyerId, rating);
        
        // 리뷰 생성 (ReviewService에서도 검증함)
        reviewService.createReview(dealId, sellerId, buyerId, rating, content);
        
        log.info("✅ 리뷰 작성 완료!");
        return "redirect:/test/review";
    }
}
