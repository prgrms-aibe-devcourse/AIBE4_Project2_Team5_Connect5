package kr.eolmago.repository.auction.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import kr.eolmago.domain.entity.auction.enums.AuctionStatus;
import kr.eolmago.dto.api.auction.response.AuctionListDto;
import kr.eolmago.repository.auction.AuctionSearchRepositoryCustom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Auction 검색 커스텀 Repository 구현체
 *
 * 역할:
 * - Native Query 기반 복잡한 검색 쿼리 구현
 * - PostgreSQL 함수 호출 (to_tsvector, word_similarity, extract_chosung)
 * - 인덱스 활용 최적화
 *
 * 검색 전략:
 * 1. Full-Text Search: 띄어쓰기 무관 검색 (GIN 인덱스)
 * 2. Trigram: 오타 교정 (GiST 인덱스)
 * 3. Chosung: 초성 검색 (함수 기반 인덱스)
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class AuctionSearchRepositoryCustomImpl implements AuctionSearchRepositoryCustom {

    private final EntityManager entityManager;

    // ============================================
    // SQL 쿼리 템플릿 상수
    // ============================================

    /**
     * 기본 SELECT 절 (모든 검색 쿼리 공통)
     */
    private static final String BASE_SELECT = """
        SELECT
            a.auction_id,
            ai.auction_item_id,
            a.title,
            img.image_url,
            up.nickname,
            a.current_price,
            a.bid_count,
            a.view_count,
            a.favorite_count,
            a.end_at,
            a.status
        FROM auctions a
        INNER JOIN auction_items ai ON a.auction_item_id = ai.auction_item_id
        INNER JOIN auction_images img ON img.auction_item_id = ai.auction_item_id AND img.display_order = 0
        INNER JOIN users u ON a.seller_id = u.user_id
        INNER JOIN user_profile up ON up.user_id = u.user_id
        """;

    /**
     * 기본 ORDER BY 절 (생성일 기준 내림차순)
     */
    private static final String ORDER_BY_CREATED = "ORDER BY a.created_at DESC ";

    /**
     * 페이징 절
     */
    private static final String PAGINATION = "LIMIT :limit OFFSET :offset";

    /**
     * 기본 COUNT 절
     */
    private static final String BASE_COUNT = "SELECT COUNT(*) FROM auctions a ";

    // ============================================
    // 검색 메서드 구현
    // ============================================

    @Override
    public Page<AuctionListDto> searchByFullText(String processedKeyword, AuctionStatus status, Pageable pageable) {
        log.debug("Full-Text Search 실행: processedKeyword={}, status={}", processedKeyword, status);

        // WHERE 조건
        String whereClause = " WHERE to_tsvector('simple', a.title || ' ' || COALESCE(a.description, '')) @@ to_tsquery('simple', :processedKeyword) ";
        String statusCondition = status != null ? "AND a.status = :status " : "";

        // 메인 쿼리 조립
        String sql = BASE_SELECT + whereClause + statusCondition + ORDER_BY_CREATED + PAGINATION;

        Query query = createQueryWithParams(sql, processedKeyword, null, 0.0, status, pageable);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();
        List<AuctionListDto> content = convertToDto(results);

        // Count 쿼리
        String countSql = BASE_COUNT + whereClause + statusCondition;
        long total = executeCountQuery(countSql, processedKeyword, null, 0.0, status);

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Page<AuctionListDto> searchByTrigram(String keyword, double threshold, AuctionStatus status, Pageable pageable) {
        log.debug("Trigram Similarity 검색 실행: keyword={}, threshold={}, status={}", keyword, threshold, status);

        // WHERE 조건
        String whereClause = "WHERE word_similarity(:keyword, a.title) > :threshold ";
        String statusCondition = status != null ? "AND a.status = :status " : "";
        String orderBy = "ORDER BY word_similarity(:keyword, a.title) DESC ";

        // 메인 쿼리 조립
        String sql = BASE_SELECT + whereClause + statusCondition + orderBy + PAGINATION;

        Query query = createQueryWithParams(sql, keyword, keyword, threshold, status, pageable);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();
        List<AuctionListDto> content = convertToDto(results);

        // Count 쿼리
        String countSql = BASE_COUNT + whereClause + statusCondition;
        long total = executeCountQuery(countSql, keyword, keyword, threshold, status);

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Page<AuctionListDto> searchByChosung(String chosungKeyword, AuctionStatus status, Pageable pageable) {
        log.debug("초성 검색 실행: chosungKeyword={}, status={}", chosungKeyword, status);

        // WHERE 조건
        String whereClause = "WHERE extract_chosung(a.title) LIKE :chosungPattern ";
        String statusCondition = status != null ? "AND a.status = :status " : "";

        // 메인 쿼리 조립
        String sql = BASE_SELECT + whereClause + statusCondition + ORDER_BY_CREATED + PAGINATION;

        Query query = createQueryWithParams(sql, chosungKeyword, null, 0.0, status, pageable);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();
        List<AuctionListDto> content = convertToDto(results);

        // Count 쿼리
        String countSql = BASE_COUNT + whereClause + statusCondition;
        long total = executeCountQuery(countSql, chosungKeyword, null, 0.0, status);

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public List<String> getSuggestedKeywords() {
        log.debug("추천 키워드 조회 실행");

        String sql = """
        WITH split_words AS (
            SELECT DISTINCT 
                regexp_split_to_table(a.title, E'\\\\s+') as word
            FROM auctions a
            WHERE a.status = 'LIVE'
        )
        SELECT word
        FROM split_words
        WHERE LENGTH(word) >= 2
            AND word !~ '^[0-9]+$'
        ORDER BY RANDOM()
        LIMIT 5
        """;

        Query query = entityManager.createNativeQuery(sql);

        @SuppressWarnings("unchecked")
        List<String> results = query.getResultList();

        return results != null ? results : new ArrayList<>();
    }

    // ============================================
    // 헬퍼 메서드
    // ============================================

    /**
     * 쿼리 생성 및 파라미터 바인딩
     *
     * @param sql SQL 쿼리
     * @param processedKeywordOrPattern 키워드 또는 패턴 (Full-Text, Chosung용)
     * @param keyword 키워드 (Trigram용)
     * @param threshold 유사도 임계값 (Trigram용)
     * @param status 경매 상태
     * @param pageable 페이징 정보
     * @return 파라미터가 바인딩된 Query 객체
     */
    private Query createQueryWithParams(
            String sql,
            String processedKeywordOrPattern,
            String keyword,
            double threshold,
            AuctionStatus status,
            Pageable pageable
    ) {
        Query query = entityManager.createNativeQuery(sql);

        // Full-Text 또는 Chosung 검색
        if (processedKeywordOrPattern != null) {
            if (sql.contains(":processedKeyword")) {
                query.setParameter("processedKeyword", processedKeywordOrPattern);
            } else if (sql.contains(":chosungPattern")) {
                query.setParameter("chosungPattern", processedKeywordOrPattern + "%");
            }
        }

        // Trigram 검색
        if (keyword != null && sql.contains(":keyword")) {
            query.setParameter("keyword", keyword);
        }

        if (threshold > 0 && sql.contains(":threshold")) {
            query.setParameter("threshold", threshold);
        }

        // 상태 필터
        if (status != null) {
            query.setParameter("status", status.name());
        }

        // 페이징
        query.setParameter("limit", pageable.getPageSize());
        query.setParameter("offset", pageable.getOffset());

        return query;
    }

    /**
     * Count 쿼리 실행
     *
     * @param countSql Count SQL
     * @param processedKeywordOrPattern 키워드 또는 패턴 (Full-Text, Chosung용)
     * @param keyword 키워드 (Trigram용)
     * @param threshold 유사도 임계값 (Trigram용)
     * @param status 경매 상태
     * @return 전체 건수
     */
    private long executeCountQuery(
            String countSql,
            String processedKeywordOrPattern,
            String keyword,
            double threshold,
            AuctionStatus status
    ) {
        Query countQuery = entityManager.createNativeQuery(countSql);

        // Full-Text 또는 Chosung 검색
        if (processedKeywordOrPattern != null) {
            if (countSql.contains(":processedKeyword")) {
                countQuery.setParameter("processedKeyword", processedKeywordOrPattern);
            } else if (countSql.contains(":chosungPattern")) {
                countQuery.setParameter("chosungPattern", processedKeywordOrPattern + "%");
            }
        }

        // Trigram 검색
        if (keyword != null && countSql.contains(":keyword")) {
            countQuery.setParameter("keyword", keyword);
        }

        if (threshold > 0 && countSql.contains(":threshold")) {
            countQuery.setParameter("threshold", threshold);
        }

        // 상태 필터
        if (status != null) {
            countQuery.setParameter("status", status.name());
        }

        return ((Number) countQuery.getSingleResult()).longValue();
    }

    /**
     * Object[] → AuctionListDto 변환
     */
    private List<AuctionListDto> convertToDto(List<Object[]> results) {
        List<AuctionListDto> dtos = new ArrayList<>();

        for (Object[] row : results) {
            AuctionListDto dto = new AuctionListDto(
                    (UUID) row[0],                              // auction_id
                    (Long) row[1],                              // auction_item_id
                    (String) row[2],                            // title
                    (String) row[3],                            // image_url
                    (String) row[4],                            // nickname
                    (Integer) row[5],                           // current_price
                    (Integer) row[6],                           // bid_count
                    (Integer) row[7],                           // view_count
                    (Integer) row[8],                           // favorite_count
                    convertToOffsetDateTime(row[9]),            // end_at
                    AuctionStatus.valueOf((String) row[10])     // status
            );
            dtos.add(dto);
        }

        return dtos;
    }

    /**
     * Object → OffsetDateTime 변환 (Timestamp 또는 Instant 처리)
     */
    private OffsetDateTime convertToOffsetDateTime(Object obj) {
        if (obj == null) {
            return null;
        }

        if (obj instanceof Timestamp) {
            return ((Timestamp) obj).toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toOffsetDateTime();
        } else if (obj instanceof java.time.Instant) {
            return ((java.time.Instant) obj)
                    .atZone(ZoneId.systemDefault())
                    .toOffsetDateTime();
        }

        return null;
    }
}
