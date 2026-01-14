package kr.eolmago.repository.auction;

import kr.eolmago.domain.entity.auction.Auction;
import kr.eolmago.domain.entity.auction.enums.AuctionStatus;
import kr.eolmago.domain.entity.auction.enums.ItemCategory;
import kr.eolmago.dto.api.auction.request.AuctionSearchRequest;
import kr.eolmago.dto.api.auction.response.AuctionDetailDto;
import kr.eolmago.dto.api.auction.response.AuctionListDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuctionRepositoryCustom {

    // 경매 목록 조회
    Page<AuctionListDto> searchList(
            Pageable pageable,
            String sortKey,
            AuctionSearchRequest searchRequest
    );

    // 경매 상세 조회
    Optional<AuctionDetailDto> findDetailById(UUID auctionId);

    Optional<Auction> findByIdForUpdate(UUID auctionId);
}
