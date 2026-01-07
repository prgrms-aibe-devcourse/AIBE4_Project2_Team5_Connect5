package kr.eolmago.repository.auction;

import kr.eolmago.domain.entity.auction.enums.AuctionStatus;
import kr.eolmago.dto.api.auction.response.AuctionListDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Auction 검색 전용 커스텀 Repository 인터페이스
 *
 * 특수문자 포함으로 Native Query 사용:
 * - PostgreSQL 함수 (to_tsvector, similarity, extract_chosung)
 * - QueryDSL 대신 @Query 사용
 *
 * 구현체:
 * - AuctionSearchRepositoryImpl
 */
public interface AuctionSearchRepositoryCustom {

    /**
     * Full-Text Search (PostgreSQL to_tsvector)
     */
    Page<AuctionListDto> searchByFullText(String keyword, AuctionStatus status, Pageable pageable);

    /**
     * Trigram Similarity (PostgreSQL pg_trgm)
     */
    Page<AuctionListDto> searchByTrigram(String keyword, double threshold, AuctionStatus status, Pageable pageable);

    /**
     * 초성 검색 (extract_chosung 함수)
     */
    Page<AuctionListDto> searchByChosung(String chosungKeyword, AuctionStatus status, Pageable pageable);

    /**
     * 추천 키워드 조회
     */
    List<String> getSuggestedKeywords();
}
