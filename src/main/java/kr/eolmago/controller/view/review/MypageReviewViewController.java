package kr.eolmago.controller.view.review;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/mypage")
public class MypageReviewViewController {

    // 내가 작성한 리뷰 목록 (buyer)
    @GetMapping("/buyer-reviews")
    public String buyerReviews() {
        return "pages/user/mypage/buyer-reviews";
    }

    // 내가 받은 리뷰 목록 (seller)
    @GetMapping("/seller-reviews")
    public String sellerReviews() {
        return "pages/user/mypage/seller-reviews";
    }

    // 내가 작성한 리뷰 상세
    @GetMapping("/buyer-reviews/{reviewId}")
    public String buyerReviewDetail(@PathVariable Long reviewId, Model model) {
        model.addAttribute("reviewId", reviewId);
        model.addAttribute("mode", "WRITTEN"); // JS에서 buyer 상세 API 호출
        return "pages/user/mypage/review-detail";
    }

    // 내가 받은 리뷰 상세
    @GetMapping("/seller-reviews/{reviewId}")
    public String sellerReviewDetail(@PathVariable Long reviewId, Model model) {
        model.addAttribute("reviewId", reviewId);
        model.addAttribute("mode", "RECEIVED"); // JS에서 seller 상세 API 호출
        return "pages/user/mypage/review-detail";
    }
}
