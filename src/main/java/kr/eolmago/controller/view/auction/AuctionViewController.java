package kr.eolmago.controller.view.auction;

import kr.eolmago.domain.entity.auction.enums.AuctionStatus;
import kr.eolmago.dto.api.auction.request.AuctionSearchRequest;
import kr.eolmago.dto.api.auction.response.AuctionListResponse;
import kr.eolmago.dto.api.common.PageResponse;
import kr.eolmago.dto.view.auction.AuctionListFilterRequest;
import kr.eolmago.global.security.CustomUserDetails;
import kr.eolmago.service.auction.AuctionSearchService;
import kr.eolmago.service.auction.AuctionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
public class AuctionViewController {

    private final AuctionService auctionService;
    private final AuctionSearchService auctionSearchService;

    @GetMapping
    public String auctionList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            @RequestParam(defaultValue = "latest") String sort,
            AuctionListFilterRequest filterRequest,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {

        String keyword = filterRequest.keyword();
        var category = filterRequest.category();
        List<String> brands = filterRequest.brands();
        Integer minPrice = filterRequest.minPrice();
        Integer maxPrice = filterRequest.maxPrice();

        // 경매 검색 및 목록은 LIVE 상태만 조회
        AuctionStatus searchStatus = AuctionStatus.LIVE;

        UUID userId = userDetails != null ? UUID.fromString(userDetails.getId()) : null;
        Pageable pageable = PageRequest.of(page, size);

        AuctionSearchRequest searchRequest = new AuctionSearchRequest(
                searchStatus,
                keyword,
                category,
                brands,
                minPrice,
                maxPrice,
                userId
        );

        PageResponse<AuctionListResponse> auctions = auctionSearchService.search(searchRequest, sort, pageable);

        // 검색 모드 분기 처리
        boolean isSearchMode = keyword != null && !keyword.trim().isEmpty();

        // 검색 결과 없을 때 추천 키워드 조회
        if (isSearchMode && auctions.pageInfo().totalElements() == 0) {
            List<String> suggestedKeywords = auctionSearchService.getSuggestedKeywords();
            model.addAttribute("suggestedKeywords", suggestedKeywords);
        }

        // 정렬 옵션
        List<Map<String, Object>> sortOptions = List.of(
                Map.of("value", "latest", "label", "최신순"),
                Map.of("value", "popular", "label", "인기순"),
                Map.of("value", "deadline", "label", "마감임박순"),
                Map.of("value", "price_low", "label", "낮은가격순"),
                Map.of("value", "price_high", "label", "높은가격순")
        );

        model.addAttribute("auctions", auctions);
        model.addAttribute("sortOptions", sortOptions);
        model.addAttribute("sort", sort);
        model.addAttribute("keyword", keyword);
        model.addAttribute("category", category);
        model.addAttribute("brands", brands);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("isSearchMode", isSearchMode);

        return "pages/auction/auction-list";
    }

    @GetMapping("/{auctionId}")
    public String auctionDetail(
            @PathVariable UUID auctionId,
            Model model,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        model.addAttribute("auctionId", auctionId);
        // userRole, userStatus는 NavModelAdvice가 자동으로 설정

        return "pages/auction/auction-detail";
    }
}
