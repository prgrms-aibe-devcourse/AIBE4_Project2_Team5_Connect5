package kr.eolmago.controller.view.buyer;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/buyer")
public class BuyerViewController {

    @GetMapping("/deals")
    public String buyerDeals() {
        return "pages/buyer/buyer-deals";
    }

    // ✅ 리뷰 작성 페이지 매핑 추가
    @GetMapping("/deals/{dealId}/review")
    public String buyerDealReviewCreate(@PathVariable Long dealId, Model model) {
        // 템플릿에서 사용할 dealId 전달
        model.addAttribute("dealId", dealId);
        return "pages/user/mypage/buyer-reviews_create";
    }
}
