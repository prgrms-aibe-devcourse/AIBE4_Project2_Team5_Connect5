package kr.eolmago.controller.api.auction;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.eolmago.domain.entity.auction.enums.AuctionStatus;
import kr.eolmago.domain.entity.auction.enums.ItemCategory;
import kr.eolmago.dto.api.auction.request.AuctionDraftRequest;
import kr.eolmago.dto.api.auction.response.AuctionDraftDetailResponse;
import kr.eolmago.dto.api.auction.response.AuctionDraftResponse;
import kr.eolmago.dto.api.auction.response.AuctionListResponse;
import kr.eolmago.dto.api.common.PageResponse;
import kr.eolmago.global.security.CustomUserDetails;
import kr.eolmago.service.auction.AuctionSearchService;
import kr.eolmago.service.auction.AuctionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/auctions")
@Tag(name = "Auction")
@RequiredArgsConstructor
@Slf4j
public class AuctionApiController {

    private final AuctionService auctionService;
    private final AuctionSearchService auctionSearchService;

    @Operation(summary = "경매 임시저장 생성")
    @PostMapping("/drafts")
    public ResponseEntity<AuctionDraftResponse> createDraft (
            @Valid @RequestBody AuctionDraftRequest request,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        UUID sellerId = UUID.fromString(principal.getId());
        AuctionDraftResponse response = auctionService.createDraft(request, sellerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "경매 임시저장 수정")
    @PutMapping("/drafts/{auctionId}")
    public ResponseEntity<AuctionDraftResponse> updateDraft (
            @PathVariable UUID auctionId,
            @Valid @RequestBody AuctionDraftRequest request,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        UUID sellerId = UUID.fromString(principal.getId());
        AuctionDraftResponse response = auctionService.updateDraft(auctionId, request, sellerId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "경매 게시")
    @PostMapping("/{auctionId}/publish")
    public ResponseEntity<AuctionDraftResponse> publish (
            @PathVariable UUID auctionId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        UUID sellerId = UUID.fromString(principal.getId());
        AuctionDraftResponse response = auctionService.publishAuction(auctionId, sellerId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "경매 삭제")
    @DeleteMapping("/{auctionId}")
    public ResponseEntity<Void> deleteAuction (
            @PathVariable UUID auctionId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        UUID sellerId = UUID.fromString(principal.getId());
        auctionService.deleteAuction(auctionId, sellerId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "경매 임시저장 초기화")
    @PostMapping("/drafts/init")
    public ResponseEntity<AuctionDraftResponse> initDraft(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        UUID sellerId = UUID.fromString(principal.getId());
        AuctionDraftResponse response = auctionService.initDraft(sellerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "경매 목록 조회",
            description = "검색어, 필터, 정렬 조건으로 경매 목록을 조회합니다. " +
                    "키워드가 없으면 전체 목록을 반환하고, 키워드가 있으면 검색 결과를 반환합니다."
    )
    @GetMapping
    public ResponseEntity<PageResponse<AuctionListResponse>> getAuctions (
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(required = false) AuctionStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ItemCategory category,
            @RequestParam(required = false) List<String> brands,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("경매 목록 조회 API: keyword={}, category={}, brands={}, minPrice={}, maxPrice={}, sort={}, page={}", keyword, category, brands, minPrice, maxPrice, sort, page);

        // 사용자ID 추출(비로그인시 null, 검색 통계용)
        UUID userId = userDetails != null ? UUID.fromString(userDetails.getId()) : null;

        Pageable pageable = PageRequest.of(page, size);

        // 키워드 있으면 검색, 없으면 전체조회 (서비스단에서)
        PageResponse<AuctionListResponse> response = auctionSearchService.search(
                keyword,
                category,
                brands,
                minPrice,
                maxPrice,
                sort,
                status,
                pageable,
                userId
        );

        log.info("경매 목록 조회 완료: totalElements={}", response.pageInfo().totalElements());

        /* PageResponse<AuctionListResponse> response = auctionService.getAuctions(page, size, sort, status, sellerId, category, brands, minPrice, maxPrice);*/

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "경매 임시저장 단건 조회")
    @GetMapping("/drafts/{auctionId}")
    public ResponseEntity<AuctionDraftDetailResponse> getDraft (
            @PathVariable UUID auctionId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        UUID sellerId = UUID.fromString(principal.getId());
        AuctionDraftDetailResponse response = auctionService.getDraft(auctionId, sellerId);
        return ResponseEntity.ok(response);
    }
}