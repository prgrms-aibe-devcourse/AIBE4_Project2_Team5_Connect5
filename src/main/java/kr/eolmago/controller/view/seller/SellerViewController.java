package kr.eolmago.controller.view.seller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;

@Controller
@RequestMapping("/seller")
public class SellerViewController {

    // 내 경매 페이지
    @GetMapping("/auctions")
    public String sellerAuctions() { return "pages/seller/seller-auctions"; }

    // 판매 거래 관리 페이지
    @GetMapping("/deals")
    public String sellerDeals() {
        return "pages/seller/seller-deals";
    }

    // 경매 등록 페이지
    @GetMapping("/auctions/create")
    public String auctionCreate() {
        return "pages/auction/auction-create";
    }

    // 경매 수정 페이지
    @GetMapping("/auctions/{auctionId}/edit")
    public String auctionEdit(@PathVariable UUID auctionId) {
        return "pages/auction/auction-edit";
    }

    // 유찰된 경매 상세 페이지
    @GetMapping("/auctions/{auctionId}/failed")
    public String auctionFailed(@PathVariable UUID auctionId) {
        return "pages/auction/auction-failed-detail";
    }
}
