package kr.eolmago.controller.view.deal;

import kr.eolmago.controller.view.support.SellerViewModelSupport;
import kr.eolmago.dto.api.deal.response.SellerDealDetailResponse;
import kr.eolmago.global.security.CustomUserDetails;
import kr.eolmago.service.deal.SellerDealService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequestMapping("/seller")
@RequiredArgsConstructor
public class DealViewController {

    private final SellerDealService sellerDealService;
    private final SellerViewModelSupport sellerViewModelSupport;

    @GetMapping("/deals")
    public String deals(Model model) {
        sellerViewModelSupport.applyCommonModel(model);
        return "pages/seller/seller-deals";
    }

    @GetMapping("/deals/{dealId}")
    public String sellerDealDetail(@PathVariable Long dealId,
                                   @AuthenticationPrincipal CustomUserDetails userDetails,
                                   Model model) {

        UUID sellerId = userDetails.getUserId();
        SellerDealDetailResponse deal = sellerDealService.getDealDetail(dealId, sellerId);

        sellerViewModelSupport.applyCommonModel(model);
        model.addAttribute("dealId", dealId);
        model.addAttribute("role", "SELLER");
        model.addAttribute("deal", deal);

        return "pages/deal/deal-detail";
    }
}
