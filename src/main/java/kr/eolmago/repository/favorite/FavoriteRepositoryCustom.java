package kr.eolmago.repository.favorite;

import kr.eolmago.dto.api.favorite.response.FavoriteAuctionDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface FavoriteRepositoryCustom {
    Page<FavoriteAuctionDto> searchMyFavorites(Pageable pageable, UUID userId, String filter, String sort);
}
