package kr.eolmago.service.auction;

import kr.eolmago.domain.entity.auction.Auction;
import kr.eolmago.domain.entity.auction.AuctionItem;
import kr.eolmago.domain.entity.auction.enums.AuctionStatus;
import kr.eolmago.domain.entity.user.User;
import kr.eolmago.dto.api.auction.request.AuctionCreateRequest;
import kr.eolmago.dto.api.auction.request.AuctionUpdateRequest;
import kr.eolmago.dto.api.auction.response.AuctionListResponse;
import kr.eolmago.dto.api.common.PageResponse;
import kr.eolmago.dto.api.auction.response.AuctionListDto;
import kr.eolmago.repository.auction.AuctionItemRepository;
import kr.eolmago.repository.auction.AuctionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuctionService {

    private final AuctionRepository auctionRepository;
    private final AuctionItemRepository auctionItemRepository;

    // 경매 조회
    public PageResponse<AuctionListResponse> getAuctions(int page, int size, String sortKey, AuctionStatus status, UUID sellerId) {
        Pageable pageable = PageRequest.of(page, size);

        Page<AuctionListDto> dtoPage = auctionRepository.searchList(pageable, sortKey, status, sellerId);
        Page<AuctionListResponse> responsePage = dtoPage.map(AuctionListResponse::from);

        return PageResponse.of(responsePage);
    }

}