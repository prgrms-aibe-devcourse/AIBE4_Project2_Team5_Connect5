package kr.eolmago.repository.auction;

import kr.eolmago.domain.entity.auction.enums.AuctionStatus;
import kr.eolmago.dto.api.auction.response.AuctionListDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AuctionRepositoryCustom {

    // 경매 목록 조회
    Page<AuctionListDto> searchList(Pageable pageable, String sortKey, AuctionStatus status, UUID sellerId);
}
