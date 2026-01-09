package kr.eolmago.repository.auction;

import kr.eolmago.domain.entity.auction.enums.AuctionStatus;
import kr.eolmago.domain.entity.auction.enums.ItemCategory;
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
 * - AuctionSearchRepositoryCustomImpl
 */
public interface AuctionSearchRepositoryCustom {

    /**
     * Full-Text Search (PostgreSQL to_tsvector, 필터링 포함)
     */
    Page<AuctionListDto> searchByFullText(
            String keyword,
            ItemCategory category,
            List<String> brands,
            Integer minPrice,
            Integer maxPrice,
            String sort,
            AuctionStatus status,
            Pageable pageable
    );

    /**
     * 오타 교정 : Trigram Similarity (PostgreSQL pg_trgm, 필터링 포함)
     */
    Page<AuctionListDto> searchByTrigram(
            String keyword,
            double threshold,
            ItemCategory category,
            List<String> brands,
            Integer minPrice,
            Integer maxPrice,
            String sort,
            AuctionStatus status,
            Pageable pageable
    );

    /**
     * 초성 검색 (extract_chosung 함수, 필터링 포함)
     */
    Page<AuctionListDto> searchByChosung(
            String chosungKeyword,
            ItemCategory category,
            List<String> brands,
            Integer minPrice,
            Integer maxPrice,
            String sort,
            AuctionStatus status,
            Pageable pageable
    );

    /**
     * 추천 키워드 조회
     */
    List<String> getSuggestedKeywords();
}
