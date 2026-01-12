package kr.eolmago.controller.view.buyer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/buyer")
public class BuyerViewController {

    // 입찰/낙찰 내역 페이지
    @GetMapping("/bids")
    public String bids() {
        return "pages/buyer/buyer-bids";
    }

    // 거래 관리 페이지
    @GetMapping("/deals")
    public String deals() {
        return "pages/buyer/buyer-deals";
    }

    // 거래 상세 페이지
    @GetMapping("/deals/{dealId}")
    public String buyerDealDetail(@PathVariable Long dealId, Model model) {
        model.addAttribute("dealId", dealId);
        return "pages/buyer/buyer-deal-detail";
    }
}
