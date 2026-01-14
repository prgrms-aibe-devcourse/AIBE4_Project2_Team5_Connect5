package kr.eolmago.controller.view.buyer;

import kr.eolmago.dto.api.deal.response.BuyerDealDetailResponse;
import kr.eolmago.global.security.CustomUserDetails;
import kr.eolmago.service.deal.BuyerDealService;
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

    // 구매자 거래 목록 페이지
    @GetMapping("/deals")
    public String deals() {
        return "pages/buyer/buyer-deals";
    }

    // 거래 상세 페이지
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

        // 리뷰 작성 페이지 템플릿
        return "pages/user/mypage/buyer-reviews_create";
    }
}
