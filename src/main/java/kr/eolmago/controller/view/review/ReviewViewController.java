package kr.eolmago.controller.view.review;

import kr.eolmago.controller.view.support.SellerViewModelSupport;
import kr.eolmago.dto.api.deal.response.BuyerDealDetailResponse;
import kr.eolmago.dto.view.review.ReviewResponse;
import kr.eolmago.global.security.CustomUserDetails;
import kr.eolmago.service.deal.BuyerDealService;
import kr.eolmago.service.review.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequestMapping
@RequiredArgsConstructor
public class ReviewViewController {

    private final ReviewService reviewService;

    // seller 공통 모델 (seller 페이지에서만 사용)
    private final SellerViewModelSupport sellerViewModelSupport;

    // buyer 리뷰 작성 페이지에서 deal을 읽어와야 함
    private final BuyerDealService buyerDealService;

    // =========================
    // Seller: 리뷰 상세 보기
    // =========================
    @GetMapping("/seller/deals/{dealId}/review/view")
    public String sellerReviewView(@PathVariable Long dealId,
                                   @AuthenticationPrincipal CustomUserDetails userDetails,
                                   Model model) {

        UUID sellerId = userDetails.getUserId();
        ReviewResponse review = reviewService.getReviewByDealIdForSeller(dealId, sellerId);

        sellerViewModelSupport.applyCommonModel(model);
        model.addAttribute("dealId", dealId);
        model.addAttribute("role", "SELLER");
        model.addAttribute("review", review);

        return "pages/reviews/review-detail";
    }

    // =========================
    // Buyer: 리뷰 작성 페이지
    // =========================
    @GetMapping("/buyer/deals/{dealId}/review")
    public String buyerDealReviewCreate(@PathVariable Long dealId,
                                        @AuthenticationPrincipal CustomUserDetails userDetails,
                                        Model model) {

        UUID buyerId = userDetails.getUserId();
        BuyerDealDetailResponse deal = buyerDealService.getDealDetail(dealId, buyerId);

        model.addAttribute("dealId", dealId);
        model.addAttribute("role", "BUYER");
        model.addAttribute("deal", deal);

        return "pages/user/mypage/buyer-reviews_create";
    }

    // =========================
    // Buyer: 리뷰 상세 보기
    // =========================
    @GetMapping("/buyer/deals/{dealId}/review/view")
    public String buyerDealReviewView(@PathVariable Long dealId,
                                      @AuthenticationPrincipal CustomUserDetails userDetails,
                                      Model model) {

        UUID buyerId = userDetails.getUserId();
        ReviewResponse review = reviewService.getReviewByDealIdForBuyer(dealId, buyerId);

        model.addAttribute("dealId", dealId);
        model.addAttribute("role", "BUYER");
        model.addAttribute("review", review);

        return "pages/reviews/review-detail";
    }
}
