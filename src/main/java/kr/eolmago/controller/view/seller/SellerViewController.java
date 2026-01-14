package kr.eolmago.controller.view.seller;

import kr.eolmago.dto.api.deal.response.SellerDealDetailResponse;
import kr.eolmago.dto.view.review.ReviewResponse;
import kr.eolmago.global.security.CustomUserDetails;
import kr.eolmago.service.deal.SellerDealService;
import kr.eolmago.service.review.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;

@Controller
@RequestMapping("/seller")
@RequiredArgsConstructor
public class SellerViewController {

    private final SellerDealService sellerDealService;
    private final ReviewService reviewService;

    @Value("${supabase.url:}")
    private String supabaseUrl;

    @Value("${supabase.anon-key:}")
    private String supabaseAnonKey;

    @GetMapping("/auctions/create")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public String auctionCreate(
            Model model,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        applyCommonModel(model);

        model.addAttribute("mode", "create");
        model.addAttribute("auctionId", null);
        model.addAttribute("statusText", "작성 중");

        return "pages/seller/create-auction";
    }

    @GetMapping("/auctions/drafts/{auctionId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public String auctionEdit(@PathVariable UUID auctionId,
                              Model model,
                              @AuthenticationPrincipal CustomUserDetails userDetails) {

        applyCommonModel(model);

        model.addAttribute("mode", "edit");
        model.addAttribute("auctionId", auctionId);
        model.addAttribute("statusText", "임시 저장");

        return "pages/seller/create-auction";
    }

    private void applyCommonModel(Model model) {
        model.addAttribute("apiBase", "/api/auctions");
        model.addAttribute("redirectAfterPublish", "/seller/auctions");
        model.addAttribute("redirectAfterDelete", "/seller/auctions");

        model.addAttribute("supabaseUrl", supabaseUrl);
        model.addAttribute("supabaseAnonKey", supabaseAnonKey);
    }

    @GetMapping("/auctions")
    public String sellerAuctions(Model model) {
        applyCommonModel(model);
        return "pages/seller/seller-auctions";
    }

    @GetMapping("/deals")
    public String deals(Model model) {
        applyCommonModel(model);
        return "pages/seller/seller-deals";
    }

    @GetMapping("/deals/{dealId}")
    public String sellerDealDetail(@PathVariable Long dealId,
                                   @AuthenticationPrincipal CustomUserDetails userDetails,
                                   Model model) {

        UUID sellerId = userDetails.getUserId();
        SellerDealDetailResponse deal = sellerDealService.getDealDetail(dealId, sellerId);

        applyCommonModel(model);
        model.addAttribute("dealId", dealId);
        model.addAttribute("role", "SELLER");
        model.addAttribute("deal", deal);

        return "pages/deal/deal-detail";
    }

    // 판매자 리뷰 상세 보기
    @GetMapping("/deals/{dealId}/review/view")
    public String sellerReviewView(@PathVariable Long dealId,
                                   @AuthenticationPrincipal CustomUserDetails userDetails,
                                   Model model) {

        UUID sellerId = userDetails.getUserId();
        ReviewResponse review = reviewService.getReviewByDealIdForSeller(dealId, sellerId);

        applyCommonModel(model);
        model.addAttribute("dealId", dealId);
        model.addAttribute("role", "SELLER");
        model.addAttribute("review", review);

        return "pages/reviews/review-detail";
    }
}
