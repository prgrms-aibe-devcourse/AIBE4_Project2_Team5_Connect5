package kr.eolmago.dto.api.favorite.response;

import java.util.Map;
import java.util.UUID;

/**
 * 배치 찜 여부 응답
 * @param favoritedByAuctionId
 */
public record FavoriteStatusResponse(
        Map<UUID, Boolean> favoritedByAuctionId
) {
}
