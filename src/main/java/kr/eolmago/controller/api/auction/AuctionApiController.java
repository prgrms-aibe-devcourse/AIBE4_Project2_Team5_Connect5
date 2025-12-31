package kr.eolmago.controller.api.auction;

import kr.eolmago.dto.api.auction.response.AuctionListResponse;
import kr.eolmago.global.common.PageResponse;
import kr.eolmago.service.auction.AuctionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 경매 REST API Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/auctions")
@RequiredArgsConstructor
public class AuctionApiController {

    private final AuctionService auctionService;

    /**
     * 진행 중인 경매 목록 조회
     *
     * GET /api/auctions?page=0&size=10&sort=latest
     *
     * @param page 페이지 번호 (기본값: 0)
     * @param size 페이지 크기 (기본값: 10)
     * @param sort 정렬 기준 (latest, deadline, price_asc, price_desc, popular)
     * @return PageResponse<AuctionListResponse>
     */
    @GetMapping
    public ResponseEntity<PageResponse<AuctionListResponse>> getAuctions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "latest") String sort
    ) {
        log.info("경매 목록 조회 요청: page={}, size={}, sort={}", page, size, sort);

        PageResponse<AuctionListResponse> response = auctionService.getActiveAuctions(page, size, sort);

        return ResponseEntity.ok(response);
    }
}