package kr.eolmago.service.auction;

import kr.eolmago.domain.entity.auction.enums.AuctionStatus;
import kr.eolmago.dto.api.auction.response.AuctionListResponse;
import kr.eolmago.dto.api.common.PageResponse;
import kr.eolmago.dto.api.auction.response.AuctionListDto;
import kr.eolmago.repository.auction.AuctionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuctionService {

    private final AuctionRepository auctionRepository;

    // 경매 생성(draft)

    // 경매 수정

    // 경매 삭제

    // 경매 게시(draft -> live)

    // 경매 조회(live)
    public PageResponse<AuctionListResponse> getAuctions(int page, int size, String sortKey) {
        Pageable pageable = PageRequest.of(page, size);

        Page<AuctionListDto> dtoPage = auctionRepository.searchList(AuctionStatus.LIVE, pageable, sortKey);
        Page<AuctionListResponse> responsePage = dtoPage.map(AuctionListResponse::from);

        return PageResponse.of(responsePage);
    }

}