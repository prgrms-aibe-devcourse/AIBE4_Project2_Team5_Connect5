package kr.eolmago.controller.api.auction;

import kr.eolmago.dto.api.auction.response.AuctionListResponse;
import kr.eolmago.dto.api.common.PageResponse;
import kr.eolmago.service.auction.AuctionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/auctions")
@RequiredArgsConstructor
public class AuctionApiController {

    private final AuctionService auctionService;

    // 경매 생성(draft)

    // 경매 수정

    // 경매 삭제

    // 경매 게시(draft -> live)

    // 경매 조회(live)
    @GetMapping
    public ResponseEntity<PageResponse<AuctionListResponse>> getAuctions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "latest") String sort
    ) {
        PageResponse<AuctionListResponse> response = auctionService.getAuctions(page, size, sort);

        return ResponseEntity.ok(response);
    }
}