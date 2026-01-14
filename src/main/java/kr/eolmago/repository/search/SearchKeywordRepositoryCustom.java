package kr.eolmago.repository.search;

import kr.eolmago.domain.entity.search.SearchKeyword;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 검색 키워드 Custom Repository (QueryDSL)
 */
public interface SearchKeywordRepositoryCustom {

    /**
     * Prefix 매칭 검색어 조회 (자동완성용)
     *
     * 동작:
     * 1. keyword가 prefix로 시작하는 검색어 필터링
     * 2. searchCount 내림차순 정렬
     * 3. 상위 limit개만 반환
     *
     * @param prefix 검색어 앞부분 (예: "아이")
     * @param limit 조회 개수 (기본 10)
     * @return 검색량 기준 상위 N개
     */
    List<SearchKeyword> findByKeywordPrefix(String prefix, int limit);

    /**
     * 인기 검색어 Top N 조회
     *
     * 동작:
     * 1. 전체 검색어 중
     * 2. searchCount 내림차순 정렬
     * 3. 상위 limit개 반환
     *
     * @param limit 조회 개수
     * @return 인기 검색어 목록
     */
    List<SearchKeyword> findTopBySearchCount(int limit);

    /**
     * 초성 prefix 검색 (자동완성 - 초성)
     *
     * @param chosungPrefix 초성 prefix
     * @param limit 최대 개수
     * @return 검색 결과 목록
     */
    List<SearchKeyword> findByChosungPrefix(String chosungPrefix, int limit);

    /**
     * 오래된 키워드 정리 (스케줄러 작업용)
     *
     * 동작:
     * 1. lastSearchedAt < threshold (예: 3개월 전)
     * 2. AND searchCount < minCount (예: 5회 미만)
     * 3. 조건 만족하는 키워드 조회
     *
     * @param threshold 기준 시간
     * @param minCount 최소 검색 횟수
     * @return 삭제 대상 키워드 목록
     */
    List<SearchKeyword> findInactiveKeywords(OffsetDateTime threshold, int minCount);

    /**
     * 검색어 통계 원자적 업데이트 (UPSERT)
     *
     * 동작:
     * - 검색어가 없으면: INSERT (search_count = 1)
     * - 검색어가 있으면: UPDATE (search_count + 1)
     * - PostgreSQL의 INSERT ... ON CONFLICT 활용
     *
     * 동시성:
     * - DB 레벨에서 원자적으로 처리 (Race Condition 없음)
     * - 트랜잭션 격리 수준에 의존하지 않음
     *
     * @param keyword 검색어
     */
    void upsertSearchCount(String keyword, String keywordType);
}
