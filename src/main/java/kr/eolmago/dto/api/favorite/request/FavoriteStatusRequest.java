package kr.eolmago.dto.api.favorite.request;

import java.util.List;
import java.util.UUID;

/**
 * 배치 찜 여부 요청
 * @param auctionIds
 */
public record FavoriteStatusRequest(
        List<UUID> auctionIds
) {
}
