package kr.eolmago.controller.view.review;

import kr.eolmago.controller.view.support.SellerViewModelSupport;
import kr.eolmago.dto.view.review.ReviewResponse;
import kr.eolmago.global.security.CustomUserDetails;
import kr.eolmago.service.review.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequestMapping("/seller")
@RequiredArgsConstructor
public class ReviewViewController {

    private final ReviewService reviewService;
    private final SellerViewModelSupport sellerViewModelSupport;

    @GetMapping("/deals/{dealId}/review/view")
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
}
