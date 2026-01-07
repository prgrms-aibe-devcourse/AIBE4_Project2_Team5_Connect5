package kr.eolmago.controller.view.buyer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 구매자 View Controller
 */
@Slf4j
@Controller
@RequestMapping("/buyer")
public class BuyerViewController {

    /**
     * 입찰/낙찰 내역 페이지
     *
     * GET /buyer/bids
     */
    @GetMapping("/bids")
    public String bids() {
        return "pages/buyer/buyer-bids";
    }

    /**
     * 거래 관리 페이지
     *
     * GET /buyer/deals
     */
    @GetMapping("/deals")
    public String deals() {
        return "pages/buyer/buyer-deals";
    }

    /**
     * 찜 목록 페이지
     *
     * GET /buyer/favorites
     */
    @GetMapping("/favorites")
    public String favorites() {
        return "pages/buyer/buyer-favorites";
    }

    /**
     * 내 리뷰 페이지
     *
     * GET /buyer/reviews
     */
    @GetMapping("/reviews")
    public String reviews() {
        return "pages/buyer/buyer-reviews";
    }
}
