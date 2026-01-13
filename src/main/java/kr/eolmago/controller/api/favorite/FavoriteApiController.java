package kr.eolmago.controller.api.favorite;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.eolmago.dto.api.common.PageResponse;
import kr.eolmago.dto.api.favorite.request.FavoriteStatusRequest;
import kr.eolmago.dto.api.favorite.response.FavoriteAuctionResponse;
import kr.eolmago.dto.api.favorite.response.FavoriteStatusResponse;
import kr.eolmago.dto.api.favorite.response.FavoriteToggleResponse;
import kr.eolmago.global.security.CustomUserDetails;
import kr.eolmago.service.favorite.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Favorite", description = "찜 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/favorites")
public class FavoriteApiController {

    private final FavoriteService favoriteService;

    @PostMapping("/{auctionId}")
    public ResponseEntity<FavoriteToggleResponse>  toggle(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID auctionId
    ) {
        FavoriteToggleResponse response = favoriteService.toggleFavorite(
                userDetails.getUserId(),
                auctionId
        );

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "찜 여부 배치 조회 (경매 목록/상세 하트 표시용)")
    @PostMapping("/status")
    public ResponseEntity<FavoriteStatusResponse> getStatuses(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody FavoriteStatusRequest request
    ) {
        return ResponseEntity.ok(
                favoriteService.getFavoriteStatuses(userDetails.getUserId(), request)
        );
    }

    @Operation(summary = "내 찜 목록 조회")
    @GetMapping("/me")
    public ResponseEntity<PageResponse<FavoriteAuctionResponse>> getMyFavorites(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault Pageable pageable,
            @RequestParam(defaultValue = "ALL") String filter,
            @RequestParam(defaultValue = "recent") String sort
    ) {
        return ResponseEntity.ok(
                favoriteService.getMyFavorites(userDetails.getUserId(), pageable, filter, sort)
        );
    }
}
