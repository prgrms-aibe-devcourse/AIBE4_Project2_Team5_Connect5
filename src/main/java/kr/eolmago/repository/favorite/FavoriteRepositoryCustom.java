package kr.eolmago.repository.favorite;

import kr.eolmago.domain.entity.auction.Favorite;
import kr.eolmago.dto.api.favorite.response.FavoriteAuctionDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FavoriteRepositoryCustom {

    Page<FavoriteAuctionDto> searchMyFavorites(Pageable pageable, UUID userId, String filter, String sort);

    Optional<Favorite> findByUserAndAuction(UUID userId, UUID auctionId);

    List<UUID> findFavoritedAuctionIds(UUID userId, List<UUID> auctionIds);

    List<UUID> findUserIdsByAuctionId(UUID auctionId);
}
