package kr.eolmago.dto.api.favorite.response;

import java.util.UUID;

public record FavoriteToggleResponse(
        UUID auctionId,
        boolean favorited,
        int favoriteCount
) {
}
