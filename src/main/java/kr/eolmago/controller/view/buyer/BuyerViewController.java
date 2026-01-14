package kr.eolmago.controller.view.buyer;

import kr.eolmago.dto.api.deal.response.BuyerDealDetailResponse;
import kr.eolmago.dto.view.review.ReviewResponse;
import kr.eolmago.global.security.CustomUserDetails;
import kr.eolmago.service.deal.BuyerDealService;
import kr.eolmago.service.review.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;

@Controller
@RequestMapping("/buyer")
@RequiredArgsConstructor
public class BuyerViewController {

    private final BuyerDealService buyerDealService;
    private final ReviewService reviewService;

    @GetMapping("/deals")
    public String deals() {
        return "pages/buyer/buyer-deals";
    }

    @GetMapping("/deals/{dealId}")
    public String buyerDealDetail(@PathVariable Long dealId,
                                  @AuthenticationPrincipal CustomUserDetails userDetails,
                                  Model model) {

        UUID buyerId = userDetails.getUserId();
        BuyerDealDetailResponse deal = buyerDealService.getDealDetail(dealId, buyerId);

        model.addAttribute("dealId", dealId);
        model.addAttribute("role", "BUYER");
        model.addAttribute("deal", deal);

        return "pages/deal/deal-detail";
    }

    // 거래 리뷰 작성 페이지
    @GetMapping("/deals/{dealId}/review")
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

    // ✅ 거래 리뷰 보기 페이지 (새로 추가)
    @GetMapping("/deals/{dealId}/review/view")
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
