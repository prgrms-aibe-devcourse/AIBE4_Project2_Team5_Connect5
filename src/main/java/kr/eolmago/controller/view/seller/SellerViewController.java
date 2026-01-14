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

    private void applyCommonModel(Model model) {
        model.addAttribute("apiBase", "/api/auctions");
        model.addAttribute("redirectAfterPublish", "/seller/auctions");
        model.addAttribute("redirectAfterDelete", "/seller/auctions");

        model.addAttribute("supabaseUrl", supabaseUrl);
        model.addAttribute("supabaseAnonKey", supabaseAnonKey);
    }


}