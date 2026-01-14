package kr.eolmago.service.search.constants;

/**
 * 검색 관련 상수
 */
public final class SearchConstants {

    // Redis Key 상수
    public static final String AUTOCOMPLETE_KEY = "autocomplete:all"; // 전체 자동완성
    public static final String SEARCH_DEDUPE_PREFIX = "search:dedupe:";  // 중복 방지

    // ==== 자동완성 ====
    public static final int AUTOCOMPLETE_LIMIT = 10;        // 자동완성 결과 개수
    public static final int AUTOCOMPLETE_REDIS_TOP = 20;    // Redis에서 가져올 상위 개수
    public static final int POPULAR_KEYWORDS_LIMIT = 10;    // 인기 검색어 개수

    // ==== Redis 점수 계산 ====
    public static final int BRAND_WEIGHT = 100;             // 브랜드 키워드 가중치
    public static final double SEARCH_INCREMENT = 1.0;      // 검색 점수 증가량

    // ==== 중복 방지 ====
    public static final int SEARCH_DEDUPE_TTL_SECONDS = 10; // 중복 검색 방지 TTL (10초)

    private SearchConstants() {
        throw new AssertionError("Cannot instantiate constants class");
    }
}