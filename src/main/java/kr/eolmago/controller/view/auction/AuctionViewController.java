package kr.eolmago.controller.view.auction;

import kr.eolmago.domain.entity.auction.enums.AuctionStatus;
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
            @RequestParam(required = false) UUID sellerId,
            @RequestParam(required = false) String keyword,  // 검색어
            @AuthenticationPrincipal CustomUserDetails userDetails,  // (검색통계용) 사용자 정보
            Model model) {

        // 경매 검색 및 목록은 LIVE 상태만 조회
        AuctionStatus searchStatus = AuctionStatus.LIVE;

        // 경매 목록 조회
        PageResponse<AuctionListResponse> auctions;

        // 검색 모드 분기 처리
        boolean isSearchMode = keyword != null && !keyword.trim().isEmpty();

        if (isSearchMode) {
            log.info("검색 모드: keyword={}, page={}", keyword, page);
            // userDetails가 null이면 비로그인 사용자
            UUID userId = userDetails != null ? userDetails.getUserId() : null;

            Pageable pageable = PageRequest.of(page, size);
            auctions = auctionSearchService.search(keyword.trim(), searchStatus, pageable, userId);

            // 검색 결과 없을 때 추천 키워드 조회
            if (auctions.pageInfo().totalElements() == 0) {
                List<String> suggestedKeywords = auctionSearchService.getSuggestedKeywords();
                model.addAttribute("suggestedKeywords", suggestedKeywords);
                log.info("검색 결과 없음, 추천 키워드: {}", suggestedKeywords);
            }

            model.addAttribute("keyword", keyword.trim());
            model.addAttribute("isSearchMode", true);
        } else {
            log.info("일반 목록 모드: page={}, sort={}", page, sort);
            auctions = auctionService.getAuctions(page, size, sort, searchStatus, sellerId);
            model.addAttribute("isSearchMode", false);
        }

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
