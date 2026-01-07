package kr.eolmago.controller.api.auction;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.eolmago.domain.entity.auction.Auction;
import kr.eolmago.domain.entity.auction.enums.AuctionStatus;
import kr.eolmago.domain.entity.user.User;
import kr.eolmago.dto.api.auction.request.AuctionCreateRequest;
import kr.eolmago.dto.api.auction.request.AuctionUpdateRequest;
import kr.eolmago.dto.api.auction.response.AuctionCreateResponse;
import kr.eolmago.dto.api.auction.response.AuctionListResponse;
import kr.eolmago.dto.api.common.PageResponse;
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

    @Operation(summary = "경매 생성")
    @PostMapping
    public ResponseEntity<AuctionCreateResponse> createAuction(
            @Valid @RequestBody AuctionCreateRequest request,
            @AuthenticationPrincipal User user
    ) {
        Auction auction = auctionService.createAuction(request, user);
        AuctionCreateResponse response = AuctionCreateResponse.of(
                user.getUserId(),
                auction.getAuctionId(),
                auction.getStatus()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "경매 수정")
    @PutMapping("/{auctionId}")
    public ResponseEntity<Void> updateAuction(
            @PathVariable UUID auctionId,
            @Valid @RequestBody AuctionUpdateRequest request,
            @AuthenticationPrincipal User user
    ) {
        auctionService.updateAuction(auctionId, request, user);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "경매 삭제")
    @DeleteMapping("/{auctionId}")
    public ResponseEntity<Void> deleteAuction(
            @PathVariable UUID auctionId,
            @AuthenticationPrincipal User user
    ) {
        auctionService.deleteAuction(auctionId, user);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "경매 게시")
    @PostMapping("/{auctionId}/publish")
    public ResponseEntity<Void> publishAuction(
            @PathVariable UUID auctionId,
            @AuthenticationPrincipal User user
    ) {
        auctionService.publishAuction(auctionId, user);
        return ResponseEntity.noContent().build();
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
}