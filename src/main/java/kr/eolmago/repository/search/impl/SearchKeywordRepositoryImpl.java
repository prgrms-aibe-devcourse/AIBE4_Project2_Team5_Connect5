package kr.eolmago.repository.search.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import kr.eolmago.domain.entity.search.QSearchKeyword;
import kr.eolmago.domain.entity.search.SearchKeyword;
import kr.eolmago.repository.search.SearchKeywordRepositoryCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;


/**
 * 검색 키워드 Custom Repository 구현체
 *
 * 역할:
 * - QueryDSL을 사용한 복잡한 쿼리 구현
 */
@Repository
@RequiredArgsConstructor
public class SearchKeywordRepositoryImpl implements SearchKeywordRepositoryCustom {
    private final JPAQueryFactory queryFactory;
    private final EntityManager entityManager;

    QSearchKeyword searchKeyword = QSearchKeyword.searchKeyword;

    /**
     * Prefix 매칭 검색어 조회
     *
     * 예시:
     * - findByKeywordPrefix("아이", 10)
     * - WHERE keyword LIKE '아이%'
     * - ORDER BY search_count DESC
     * - LIMIT 10
     *
     * 연결 부분:
     * - Redis 장애 시 Fallback으로 사용
     * - SearchKeywordService.getAutocomplete()에서 호출
     */
    @Override
    public List<SearchKeyword> findByKeywordPrefix(String prefix, int limit) {
        return queryFactory
                .selectFrom(searchKeyword)
                .where(searchKeyword.keyword.startsWith(prefix))
                .orderBy(searchKeyword.searchCount.desc())
                .limit(limit) // 상위 N개만
                .fetch();
    }

    /**
     * 인기 검색어 Top N 조회
     *
     * 예시:
     * - findTopBySearchCount(10)
     * - ORDER BY search_count DESC
     * - LIMIT 10
     *
     * 연결 부분:
     * - 메인 페이지 "인기 검색어" 표시
     * - SearchKeywordService.getPopularKeywords()에서 호출
     */
    @Override
    public List<SearchKeyword> findTopBySearchCount(int limit) {
        return queryFactory
                .selectFrom(searchKeyword)
                .orderBy(searchKeyword.searchCount.desc())
                .limit(limit)
                .fetch();
    }

    @Override
    public List<SearchKeyword> findByChosungPrefix(String chosungPrefix, int limit) {
        return queryFactory
                .selectFrom(searchKeyword)
                .where(searchKeyword.keywordChosung.startsWith(chosungPrefix))
                .orderBy(searchKeyword.searchCount.desc())
                .limit(limit)
                .fetch();
    }

    /**
     * 오래된 키워드 정리
     *
     * 예시:
     * - findInactiveKeywords(3개월전, 5)
     * - WHERE last_searched_at < '2024-10-01' AND search_count < 5
     *
     * 연결 부분:
     * - 배치 작업에서 호출
     * - 데이터 정리로 DB/Redis 최적화
     *
     * 확장 가능:
     * - 나중에 OR 조건 추가 (예: 특정 타입만 삭제)
     * - 동적 조건 추가 (예: 관리자 설정값 기반)
     */
    @Override
    public List<SearchKeyword> findInactiveKeywords(OffsetDateTime threshold, int minCount) {
        return queryFactory
                .selectFrom(searchKeyword)
                .where(
                        // 오래된 검색어: lastSearchedAt < threshold
                        searchKeyword.lastSearchedAt.lt(threshold),

                        // AND 검색량 적은 검색어: searchCount < minCount
                        searchKeyword.searchCount.lt(minCount)
                )
                .fetch();
    }

    /**
     * 검색어 통계 원자적 업데이트 (UPSERT)
     *
     * PostgreSQL의 INSERT ... ON CONFLICT DO UPDATE 활용
     *
     * 동작:
     * 1. 검색어가 없으면 (INSERT):
     *    - keyword, search_count=1, keyword_type(자동판단), last_searched_at=현재시간, created_at=현재시간
     *
     * 2. 검색어가 있으면 (UPDATE):
     *    - search_count = search_count + 1
     *    - last_searched_at = 현재시간
     *
     * 동시성 보장:
     * - DB 레벨에서 원자적으로 처리 (ACID 보장)
     * - Race Condition 없음
     * - Lost Update 없음
     *
     * keyword_type 판단 로직:
     * - BRAND: 브랜드명 포함 (아이폰|갤럭시|픽셀|샤오미|apple|samsung|google|xiaomi)
     * - MODEL: 숫자 포함
     * - GENERAL: 그 외
     *
     * @param keyword 검색어
     */
    @Override
    public void upsertSearchCount(String keyword, String keywordType) {
        String sql = """
            INSERT INTO search_keywords (keyword, search_count, keyword_type, last_searched_at, created_at)
            VALUES (
                :keyword,
                1,
                :keywordType,
                NOW(),
                NOW()
            )
            ON CONFLICT (keyword) DO UPDATE SET
                search_count = search_keywords.search_count + 1,
                last_searched_at = NOW()
            """;

        entityManager.createNativeQuery(sql)
                .setParameter("keyword", keyword)
                .setParameter("keywordType", keywordType)
                .executeUpdate();
    }
}
