package kr.eolmago.controller.view.auction;

import kr.eolmago.domain.entity.auction.enums.AuctionStatus;
import kr.eolmago.domain.entity.auction.enums.ItemCategory;
import kr.eolmago.dto.api.auction.response.AuctionListResponse;
import kr.eolmago.dto.api.common.PageResponse;
import kr.eolmago.global.security.CustomUserDetails;
import kr.eolmago.service.auction.AuctionSearchService;
import kr.eolmago.service.auction.AuctionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class AuctionViewController {

    private final AuctionService auctionService;
    private final AuctionSearchService auctionSearchService;

    @GetMapping
    public String auctionList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            @RequestParam(defaultValue = "latest") String sort,
//            @RequestParam(required = false) UUID sellerId,
            @RequestParam(required = false) String keyword,  // 검색어
            @RequestParam(required = false) ItemCategory category,
            @RequestParam(required = false) List<String> brands,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @AuthenticationPrincipal CustomUserDetails userDetails,  // (검색통계용) 사용자 정보
            Model model) {

        log.info("경매 목록 조회 API: keyword={}, category={}, brands={}, minPrice={}, maxPrice={}, sort={}, page={}", keyword, category, brands, minPrice, maxPrice, sort, page);

        // 경매 검색 및 목록은 LIVE 상태만 조회
        AuctionStatus searchStatus = AuctionStatus.LIVE;

        // 사용자ID 추출(비로그인시 null, 검색 통계용)
        UUID userId = userDetails != null ? UUID.fromString(userDetails.getId()) : null;

        // Pageable 생성
        Pageable pageable = PageRequest.of(page, size);

        // 키워드 있으면 검색, 없으면 전체조회 (서비스단에서)
        PageResponse<AuctionListResponse> auctions = auctionSearchService.search(
                keyword,
                category,
                brands,
                minPrice,
                maxPrice,
                sort,
                searchStatus,
                pageable,
                userId
        );

        // 검색 모드 분기 처리
        boolean isSearchMode = keyword != null && !keyword.trim().isEmpty();

        // 검색 결과 없을 때 추천 키워드 조회
        if (isSearchMode && auctions.pageInfo().totalElements() == 0) {
            List<String> suggestedKeywords = auctionSearchService.getSuggestedKeywords();
            model.addAttribute("suggestedKeywords", suggestedKeywords);
            log.info("검색 결과 없음, 추천 키워드: {}", suggestedKeywords);
        }

        log.info("경매 목록 조회 - 총 {}개, 현재 페이지: {}",
                auctions.pageInfo().totalElements(),
                auctions.pageInfo().currentPage());

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
    public String auctionDetail(@PathVariable UUID auctionId) {
        return "pages/auction/detail";
    }
}
