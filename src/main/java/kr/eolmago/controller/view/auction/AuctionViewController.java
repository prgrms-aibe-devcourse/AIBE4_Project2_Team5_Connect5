package kr.eolmago.controller.view.auction;

import kr.eolmago.domain.entity.auction.enums.AuctionStatus;
import kr.eolmago.dto.api.auction.response.AuctionListResponse;
import kr.eolmago.dto.api.common.PageResponse;
import kr.eolmago.service.auction.AuctionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/auctions")
@RequiredArgsConstructor
@Slf4j
public class AuctionViewController {

    private final AuctionService auctionService;

    @GetMapping
    public String auctionList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(required = false) UUID sellerId,
            Model model) {

        // 경매 목록은 LIVE 상태만 조회
        AuctionStatus searchStatus = AuctionStatus.LIVE;

        // 경매 목록 조회
        PageResponse<AuctionListResponse> auctions = auctionService.getAuctions(page, size, sort, searchStatus, sellerId);

        log.info("경매 목록 조회 - 총 {}개, 현재 페이지: {}",
                auctions.pageInfo().totalElements(),
                auctions.pageInfo().currentPage());

        // 정렬 옵션
        List<Map<String, Object>> sortOptions = List.of(
                Map.of("value", "latest", "label", "최신순"),
                Map.of("value", "popular", "label", "인기순"),
                Map.of("value", "deadline", "label", "마감임박순"),
                Map.of("value", "highPrice", "label", "높은가격순"),
                Map.of("value", "lowPrice", "label", "낮은가격순")
        );

        model.addAttribute("auctions", auctions);
        model.addAttribute("sortOptions", sortOptions);
        model.addAttribute("sort", sort);

        return "pages/auction/auction-list";
    }

    @GetMapping("/{auctionId}")
    public String auctionDetail(@PathVariable UUID auctionId) {
        return "pages/auction/detail";
    }
}
