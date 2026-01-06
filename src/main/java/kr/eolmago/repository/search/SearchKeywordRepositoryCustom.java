package kr.eolmago.repository.search;

import kr.eolmago.domain.entity.search.SearchKeyword;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 검색 키워드 Custom Repository (QueryDSL)
 *
 * 역할:
 * - 복잡한 동적 쿼리
 *
 * 연결 부분:
 * - SearchKeywordRepositoryImpl에서 구현
 * - SearchKeywordService에서 사용
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
     * QueryDSL 사용 이유:
     * - 타입 안전성 (필드명 변경 시 컴파일 에러)
     * - limit 값 동적 설정 가능
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
     * QueryDSL 사용 이유:
     * - limit 동적 설정
     * - 정렬 기준 추가 시 유연함
     *
     * @param limit 조회 개수
     * @return 인기 검색어 목록
     */
    List<SearchKeyword> findTopBySearchCount(int limit);

    /**
     * 초성 prefix 검색 (자동완성 - 초성)
     *
     * 예시:
     * - "ㅇㅍ" 검색 → keyword_chosung LIKE 'ㅇㅍ%'
     *
     * @param chosungPrefix 초성 prefix
     * @param limit 최대 개수
     * @return 검색 결과 목록
     */
    List<SearchKeyword> findByChosungPrefix(String chosungPrefix, int limit);

    /**
     * 오래된 키워드 정리 (배치 작업용)
     *
     * 동작:
     * 1. lastSearchedAt < threshold (예: 3개월 전)
     * 2. AND searchCount < minCount (예: 5회 미만)
     * 3. 조건 만족하는 키워드 조회
     *
     * QueryDSL 사용 이유:
     * - 복합 조건 (AND, 비교 연산)
     * - 동적 쿼리 확장 가능 (나중에 OR 조건 추가 등)
     *
     * @param threshold 기준 시간
     * @param minCount 최소 검색 횟수
     * @return 삭제 대상 키워드 목록
     */
    List<SearchKeyword> findInactiveKeywords(OffsetDateTime threshold, int minCount);

}
