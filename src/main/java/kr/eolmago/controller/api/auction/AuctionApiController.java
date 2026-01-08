package kr.eolmago.controller.api.auction;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.eolmago.domain.entity.auction.enums.AuctionStatus;
import kr.eolmago.dto.api.auction.request.AuctionDraftRequest;
import kr.eolmago.dto.api.auction.response.AuctionDraftDetailResponse;
import kr.eolmago.dto.api.auction.response.AuctionDraftResponse;
import kr.eolmago.dto.api.auction.response.AuctionListResponse;
import kr.eolmago.dto.api.common.PageResponse;
import kr.eolmago.global.security.CustomUserDetails;
import kr.eolmago.service.auction.AuctionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auctions")
@Tag(name = "Auction")
@RequiredArgsConstructor
public class AuctionApiController {

    private final AuctionService auctionService;

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

    @Operation(summary = "경매 목록 조회")
    @GetMapping
    public ResponseEntity<PageResponse<AuctionListResponse>> getAuctions (
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(required = false) AuctionStatus status,
            @RequestParam(required = false) UUID sellerId
    ) {
        PageResponse<AuctionListResponse> response = auctionService.getAuctions(page, size, sort, status, sellerId);

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