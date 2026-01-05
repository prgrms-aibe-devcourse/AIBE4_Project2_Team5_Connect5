package kr.eolmago.service.auction;

import kr.eolmago.domain.entity.auction.Auction;
import kr.eolmago.domain.entity.auction.enums.AuctionStatus;
import kr.eolmago.dto.api.auction.response.AuctionListResponse;
import kr.eolmago.global.common.dto.PageResponse;
import kr.eolmago.repository.auction.AuctionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuctionService {

    private final AuctionRepository auctionRepository;

    /**
     * 진행 중인 경매 목록 조회 (페이징)
     *
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @param sort 정렬 기준 ("latest", "deadline", "price_asc", "price_desc", "popular")
     * @return PageResponse<AuctionListResponse>
     */
    public PageResponse<AuctionListResponse> getActiveAuctions(int page, int size, String sort) {
        // 정렬 조건 생성
        Sort sortCondition = createSort(sort);

        // Pageable 생성
        Pageable pageable = PageRequest.of(page, size, sortCondition);

        // 조회
        Page<Auction> auctionPage = auctionRepository.findByStatus(AuctionStatus.LIVE, pageable);

        // DTO 변환 + PageResponse 생성
        return PageResponse.of(auctionPage, AuctionListResponse::from);
    }

    /**
     * 전체 경매 목록 조회 (페이징)
     */
    public PageResponse<AuctionListResponse> getAllAuctions(int page, int size, String sort) {
        Sort sortCondition = createSort(sort);
        Pageable pageable = PageRequest.of(page, size, sortCondition);

        Page<Auction> auctionPage = auctionRepository.findAll(pageable);

        return PageResponse.of(auctionPage, AuctionListResponse::from);
    }

    /**
     * 정렬 조건 생성
     */
    private Sort createSort(String sort) {
        if (sort == null || sort.isEmpty()) {
            sort = "latest";  // 기본값
        }

        return switch (sort.toLowerCase()) {
            case "latest" -> Sort.by("createdAt").descending();
            case "deadline" -> Sort.by("endAt").ascending();
            case "price_asc" -> Sort.by("currentPrice").ascending();
            case "price_desc" -> Sort.by("currentPrice").descending();
            case "popular" -> Sort.by("viewCount").descending()
                    .and(Sort.by("bidCount").descending());
            default -> Sort.by("createdAt").descending();
        };
    }

}