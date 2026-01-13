package kr.eolmago.controller.view.seller;

import kr.eolmago.global.security.CustomUserDetails;
import kr.eolmago.service.deal.SellerDealService;
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

    // 내 경매 페이지
    @GetMapping("/auctions")
    public String sellerAuctions(Model model) {
        applyCommonModel(model);
        return "pages/seller/seller-auctions";
    }

    // 판매 거래 관리 페이지
    @GetMapping("/deals")
    public String sellerDeals(Model model) {
        applyCommonModel(model);
        return "pages/seller/seller-deals";
    }

    // 판매 거래 상세 페이지 (통합 페이지 사용)
    @GetMapping("/deals/{dealId}")
    public String sellerDealDetail(@PathVariable Long dealId,
                                    @AuthenticationPrincipal CustomUserDetails userDetails,
                                    Model model) {
        UUID sellerId = userDetails.getUserId();
        var deal = sellerDealService.getDealDetail(dealId, sellerId);

        model.addAttribute("dealId", dealId);
        model.addAttribute("role", "SELLER");
        model.addAttribute("deal", deal);
        return "pages/deal/deal-detail";
    }

//    // 유찰된 경매 상세 페이지 (예전 주석 예시)
//    @GetMapping("/auctions/{auctionId}/failed")
//    public String auctionFailed(@PathVariable UUID auctionId) {
//        return "pages/seller/auction-failed-detail";
//    }
}
