package kr.eolmago.repository.auction.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import kr.eolmago.domain.entity.auction.enums.AuctionEndReason;
import kr.eolmago.domain.entity.auction.enums.AuctionStatus;
import kr.eolmago.domain.entity.auction.enums.ItemCategory;
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
import java.util.*;
import java.util.stream.Collectors;

/**
 * Auction 검색 커스텀 Repository 구현체
 *
 * 역할:
 * - 특수문자 포함으로 Native Query 기반 복잡한 검색 쿼리 구현
 * - PostgreSQL 함수 호출 (to_tsvector(), word_similarity(), extract_chosung())
 * - 필터링 (카테고리, 브랜드, 가격 범위) 및 정렬
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
            ai.item_name,
            a.title,
            img.image_url,
            up.nickname,
            a.start_price,
            a.current_price,
            a.final_price,
            a.bid_count,
            a.favorite_count,
            a.end_at,
            a.status,
            a.end_reason
        FROM auctions a
        INNER JOIN auction_items ai ON a.auction_item_id = ai.auction_item_id
        INNER JOIN auction_images img ON img.auction_item_id = ai.auction_item_id AND img.display_order = 0
        INNER JOIN users u ON a.seller_id = u.user_id
        INNER JOIN user_profile up ON up.user_id = u.user_id
        """;

    /**
     * 페이징 절
     */
    private static final String PAGINATION = "LIMIT :limit OFFSET :offset";

    /**
     * 기본 COUNT 절
     */
    private static final String BASE_COUNT = """
        SELECT COUNT(*) 
        FROM auctions a
        INNER JOIN auction_items ai ON a.auction_item_id = ai.auction_item_id
        """;

    // ============================================
    // 검색 메서드 구현
    // ============================================

    @Override
    public Page<AuctionListDto> searchByFullText(
            String processedKeyword,
            ItemCategory category,
            List<String> brands,
            Integer minPrice,
            Integer maxPrice,
            String sort,
            AuctionStatus status,
            Pageable pageable
    ) {
        log.debug("Full-Text Search: keyword={}, category={}, brands={}, sort={}", processedKeyword, category, brands, sort);

        // WHERE 절 생성
        String whereClause = buildWhereClause(category, brands, minPrice, maxPrice, status);
        // item_name, title 검색
        whereClause += """
            AND (
                to_tsvector('simple', ai.item_name) @@ to_tsquery('simple', :processedKeyword)
                OR to_tsvector('simple', COALESCE(a.title, '')) @@ to_tsquery('simple', :processedKeyword)
            ) """;

        // ORDER BY 절 생성
        String orderBy = buildOrderBy(sort);

        // 메인 쿼리 조립
        String sql = BASE_SELECT + whereClause + orderBy + PAGINATION;

        // 파라미터 생성
        Map<String, Object> params = new HashMap<>();
        params.put("processedKeyword", processedKeyword);
        addCommonParams(params, category, minPrice, maxPrice, status, pageable);

        // 쿼리 실행
        Query query = createQueryWithParams(sql, params);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();
        List<AuctionListDto> content = convertToDto(results);

        // Count 쿼리
        String countSql = BASE_COUNT + whereClause;
        long total = executeCountQuery(countSql, params);

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Page<AuctionListDto> searchByTrigram(
            String keyword,
            double threshold,
            ItemCategory category,
            List<String> brands,
            Integer minPrice,
            Integer maxPrice,
            String sort,
            AuctionStatus status,
            Pageable pageable
    ) {
        log.debug("Trigram Similarity 검색 실행: keyword={}, threshold={}, category={}, brands={}, sort={}",
                keyword, threshold, category, brands, sort);

        // WHERE 절 생성
        String whereClause = buildWhereClause(category, brands, minPrice, maxPrice, status);
        // item_name 또는 title 중 하나라도 유사도가 높으면 검색 결과에 포함
        whereClause += "AND GREATEST(word_similarity(:keyword, ai.item_name), word_similarity(:keyword, COALESCE(a.title, ''))) > :threshold ";

        // ORDER BY 절 (Trigram 유사도 우선 - 가장 높은 유사도 기준)
        String orderBy = "ORDER BY GREATEST(word_similarity(:keyword, ai.item_name), word_similarity(:keyword, COALESCE(a.title, ''))) DESC ";

        // 메인 쿼리 조립
        String sql = BASE_SELECT + whereClause + orderBy + PAGINATION;

        // 파라미터 생성
        Map<String, Object> params = new HashMap<>();
        params.put("keyword", keyword);
        params.put("threshold", threshold);
        addCommonParams(params, category, minPrice, maxPrice, status, pageable);

        // 쿼리 실행
        Query query = createQueryWithParams(sql, params);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();
        List<AuctionListDto> content = convertToDto(results);

        // Count 쿼리
        String countSql = BASE_COUNT + whereClause;
        long total = executeCountQuery(countSql, params);

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Page<AuctionListDto> searchByChosung(
            String chosungKeyword,
            ItemCategory category,
            List<String> brands,
            Integer minPrice,
            Integer maxPrice,
            String sort,
            AuctionStatus status,
            Pageable pageable
    ) {
        log.debug("초성 검색 실행: chosungKeyword={}, category={}, brands={}, sort={}, status={}",
                chosungKeyword, category, brands, sort, status);

        // WHERE 절 생성
        String whereClause = buildWhereClause(category, brands, minPrice, maxPrice, status);
        // item_name 또는 title 중 하나라도 초성이 매칭되면 검색 결과에 포함
        whereClause += "AND (extract_chosung(ai.item_name) LIKE :chosungPattern OR extract_chosung(COALESCE(a.title, '')) LIKE :chosungPattern) ";

        // ORDER BY 절 생성
        String orderBy = buildOrderBy(sort);

        // 메인 쿼리 조립
        String sql = BASE_SELECT + whereClause + orderBy + PAGINATION;

        // 파라미터 생성
        Map<String, Object> params = new HashMap<>();
        params.put("chosungPattern", chosungKeyword + "%");
        addCommonParams(params, category, minPrice, maxPrice, status, pageable);

        // 쿼리 실행
        Query query = createQueryWithParams(sql, params);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();
        List<AuctionListDto> content = convertToDto(results);

        // Count 쿼리
        String countSql = BASE_COUNT + whereClause;
        long total = executeCountQuery(countSql, params);

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
     * WHERE 절 생성 (공통 필터 로직)
     */
    private String buildWhereClause(
            ItemCategory category,
            List<String> brands,
            Integer minPrice,
            Integer maxPrice,
            AuctionStatus status
    ) {
        StringBuilder where = new StringBuilder("WHERE 1=1 ");

        // Status
        if (status != null) {
            where.append("AND a.status = :status ");
        }

        // Category
        if (category != null) {
            where.append("AND ai.category = :category ");
        }

        // Brands
        where.append(buildBrandCondition(brands));

        // Price Range
        if (minPrice != null) {
            where.append("AND a.current_price >= :minPrice ");
        }
        if (maxPrice != null) {
            where.append("AND a.current_price <= :maxPrice ");
        }

        return where.toString();
    }

    /**
     * 브랜드 필터 WHERE 절 생성
     *
     * 로직:
     * - brands가 null/빈 리스트 → 전체 브랜드
     * - "기타"만 선택 → Apple, Samsung 제외
     * - "기타" + 특정 브랜드 → OR 조건
     * - 특정 브랜드만 → IN 조건
     */
    private String buildBrandCondition(List<String> brands) {
        if (brands == null || brands.isEmpty()) {
            return "";  // 전체 브랜드
        }

        boolean hasOther = brands.contains("기타");
        List<String> selectedBrands = brands.stream()
                .filter(b -> !"기타".equals(b))
                .toList();

        if (hasOther && selectedBrands.isEmpty()) {
            // "기타"만 선택: Apple, Samsung 제외한 모든 브랜드
            return "AND ai.specs->>'brand' NOT IN ('Apple', 'Samsung') ";

        } else if (hasOther && !selectedBrands.isEmpty()) {
            // "기타" + 특정 브랜드 (예: Apple + 기타)
            String brandsIn = selectedBrands.stream()
                    .map(b -> "'" + b + "'")
                    .collect(Collectors.joining(", "));
            return String.format(
                    "AND (ai.specs->>'brand' IN (%s) OR ai.specs->>'brand' NOT IN ('Apple', 'Samsung'))",
                    brandsIn
            );
        } else {
            // 특정 브랜드만 선택 (예: Apple만, Samsung만)
            String brandsIn = selectedBrands.stream()
                    .map(b -> "'" + b + "'")
                    .collect(Collectors.joining(", "));
            return String.format("AND ai.specs->>'brand' IN (%s) ", brandsIn);
        }
    }

    /**
     * ORDER BY 절 생성
     *
     * 정렬 옵션:
     * - latest: 최신순 (created_at DESC)
     * - popular: 인기순 (bid_count DESC)
     * - deadline: 마감임박순 (end_at ASC)
     * - price_low: 낮은가격순 (current_price ASC)
     * - price_high: 높은가격순 (current_price DESC)
     */
    private String buildOrderBy(String sort) {
        if (sort == null || sort.isEmpty()) {
            sort = "latest";  // 기본값
        }

        return switch (sort) {
            case "popular" -> "ORDER BY a.bid_count DESC, a.created_at DESC ";
            case "deadline" -> "ORDER BY a.end_at ASC, a.created_at DESC ";
            case "price_low" -> "ORDER BY a.current_price ASC, a.created_at DESC ";
            case "price_high" -> "ORDER BY a.current_price DESC, a.created_at DESC ";
            default -> "ORDER BY a.created_at DESC ";  // latest
        };
    }

    /**
     * 공통 파라미터 추가
     */
    private void addCommonParams(
            Map<String, Object> params,
            ItemCategory category,
            Integer minPrice,
            Integer maxPrice,
            AuctionStatus status,
            Pageable pageable
    ) {
        if (status != null) {
            params.put("status", status.name());
        }
        if (category != null) {
            params.put("category", category.name());
        }
        if (minPrice != null) {
            params.put("minPrice", minPrice);
        }
        if (maxPrice != null) {
            params.put("maxPrice", maxPrice);
        }

        params.put("limit", pageable.getPageSize());
        params.put("offset", pageable.getOffset());
    }

    /**
     * 쿼리 생성 및 파라미터 바인딩
     */
    private Query createQueryWithParams(String sql, Map<String, Object> params) {
        Query query = entityManager.createNativeQuery(sql);

        for (Map.Entry<String, Object> entry : params.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }

        return query;
    }

    /**
     * Count 쿼리 실행
     */
    private long executeCountQuery(String countSql, Map<String, Object> params) {
        Query countQuery = entityManager.createNativeQuery(countSql);

        for (Map.Entry<String, Object> entry : params.entrySet()) {
            // limit, offset은 count 쿼리에 불필요
            if (!"limit".equals(entry.getKey()) && !"offset".equals(entry.getKey())) {
                countQuery.setParameter(entry.getKey(), entry.getValue());
            }
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
                    (String) row[2],                            // item_name
                    (String) row[3],                            // title
                    (String) row[4],                            // image_url
                    (String) row[5],                            // nickname
                    (Integer) row[6],                           // start_price
                    (Integer) row[7],                           // current_price
                    (Long) row[8],                              // final_price
                    (Integer) row[9],                           // bid_count
                    (Integer) row[10],                           // favorite_count
                    convertToOffsetDateTime(row[11]),            // end_at
                    AuctionStatus.valueOf((String) row[12]),     // status
                    row[13] != null ? AuctionEndReason.valueOf((String) row[13]) : null   // endReason
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
