package kr.eolmago.controller.view.seller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;

/**
 * 판매자 페이지 View Controller
 */
@Slf4j
@Controller
@RequestMapping("/seller")
public class SellerViewController {

    /**
     * 판매 경매 페이지
     *
     * GET /seller/auctions
     */
    @GetMapping("/auctions")
    public String sellerAuctions() {
        return "pages/seller/seller-auctions";
    }

    /**
     * 판매 거래 관리 페이지
     *
     * GET /seller/deals
     */
    @GetMapping("/deals")
    public String sellerDeals() {
        return "pages/seller/seller-deals";
    }

    /**
     * 받은 리뷰 페이지
     *
     * GET /seller/reviews
     */
    @GetMapping("/reviews")
    public String sellerReviews() {
        return "pages/seller/seller-reviews";
    }

    /**
     * 새 경매 등록 페이지
     *
     * GET /seller/auctions/create
     */
    @GetMapping("/auctions/create")
    public String auctionCreate() {
        return "pages/auction/auction-create";
    }

    /**
     * 경매 수정 페이지
     *
     * GET /seller/auctions/{auctionId}/edit
     */
    @GetMapping("/auctions/{auctionId}/edit")
    public String auctionEdit(@PathVariable UUID auctionId) {
        return "pages/auction/auction-edit";
    }

    /**
     * 유찰된 경매 상세 페이지
     *
     * GET /seller/auctions/{auctionId}/failed
     */
    @GetMapping("/auctions/{auctionId}/failed")
    public String auctionFailed(@PathVariable UUID auctionId) {
        return "pages/auction/auction-failed-detail";
    }
}
