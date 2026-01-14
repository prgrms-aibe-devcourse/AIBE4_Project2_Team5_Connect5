package kr.eolmago.service.search;

import kr.eolmago.domain.entity.search.SearchKeyword;
import kr.eolmago.domain.entity.search.enums.KeywordType;
import kr.eolmago.dto.api.search.response.AutocompleteResponse;
import kr.eolmago.dto.api.search.response.PopularKeywordResponse;
import kr.eolmago.global.util.ChosungUtils;
import kr.eolmago.repository.search.SearchKeywordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static kr.eolmago.service.search.constants.SearchConstants.*;

/**
 * 검색 부가 서비스
 *
 * 핵심 역할:
 * 1. Redis 기반 실시간 자동완성 제공
 * 2. PostgreSQL에 검색어 통계 영구 저장
 * 3. 검색 중복 방지 (1분 TTL)
 * 4. 인기 검색어 제공
 *
 * 아키텍처:
 * - Redis: 빠른 자동완성 (O(log N)), 실시간 통계
 * - PostgreSQL: 백업, 영구 저장, 복잡한 쿼리
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SearchKeywordService {

    private final SearchKeywordRepository searchKeywordRepository;
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 자동완성 조회
     *
     * 동작 흐름:
     * 1. 초성 검색 여부 판단
     *    - 초성만 입력 (예: "ㅇㅇㅍ") → DB 직접 조회 (PostgreSQL 초성 함수 사용)
     *    - 일반 텍스트 (예: "아이폰") → Redis → DB Fallback
     * 2. Redis에서 prefix로 시작하는 검색어 조회
     * 3. 점수(검색량) 기준 내림차순 정렬
     * 4. 상위 10개 반환
     *
     * Redis 구조:
     * - Key: "autocomplete:all"
     * - Type: Sorted Set
     * - Member: "아이폰", "아이폰 14"
     * - Score: 검색량 + 브랜드가중치 + 정확도
     * - recordSearch()에서 Redis 업데이트
     *
     * @param prefix 검색어 앞부분 (예: "아이" 또는 "ㅇㅇ")
     * @return 자동완성 후보 목록 (최대 10개)
     */
    public List<AutocompleteResponse> getAutoComplete(String prefix) {
        log.debug("자동완성 조회: prefix={}", prefix);

        // 초성 검색은 Redis를 건너뛰고 바로 DB 조회
        if (ChosungUtils.isChosungOnly(prefix)) {
            log.debug("초성 검색 감지 → DB 직접 조회: prefix={}", prefix);
            return getFallbackAutoComplete(prefix);
        }

        // 일반 검색: Redis에서 자동완성 조회 (O(log N))
        ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();

        try {
            // 점수 역순 정렬(높은 점수=인기 검색어)
            Set<ZSetOperations.TypedTuple<String>> results =
                    zSetOps.reverseRangeByScoreWithScores(
                            AUTOCOMPLETE_KEY,
                            Double.NEGATIVE_INFINITY,
                            Double.POSITIVE_INFINITY,
                            0, AUTOCOMPLETE_REDIS_TOP
                    );

            if (results == null || results.isEmpty()) {
                // Redis 실패 시 DB Fallback
                log.debug("Redis 자동완성 결과 없음, DB Fallback 실행");
                return getFallbackAutoComplete(prefix);
            }

            // Redis 결과 -> DTO 변환
            return results.stream()
                    .filter(tuple -> tuple.getValue() != null && tuple.getValue().startsWith(prefix))
                    .map(tuple -> AutocompleteResponse.of(
                            tuple.getValue(),
                            tuple.getScore()
                    ))
                    .limit(AUTOCOMPLETE_LIMIT)
                    .toList();

        } catch (Exception e) {
            log.error("Redis 자동완성 실패, DB Fallback: prefix={}", prefix, e);
            return getFallbackAutoComplete(prefix);
        }
    }

    /**
     * DB Fallback 자동완성
     *
     * 호출 시점:
     * - Redis 장애
     * - Redis에 데이터 없음
     *
     * 동작:
     * 1. 초성만 입력된 경우 → 초성 검색
     * 2. 일반 텍스트 입력 → 일반 prefix 검색
     *
     * @param prefix 검색어 앞부분
     * @return DB 조회 결과
     */
    private List<AutocompleteResponse> getFallbackAutoComplete(String prefix) {
        List<SearchKeyword> keywords;

        // 초성 검색 여부 판단
        if (ChosungUtils.isChosungOnly(prefix)) {
            log.debug("초성 검색: prefix={}", prefix);
            keywords = searchKeywordRepository.findByChosungPrefix(prefix, AUTOCOMPLETE_LIMIT);
        } else {
            log.debug("일반 검색: prefix={}", prefix);
            keywords = searchKeywordRepository.findByKeywordPrefix(prefix, AUTOCOMPLETE_LIMIT);
        }

        return keywords.stream()
                .map(AutocompleteResponse::from)
                .toList();
    }

    /**
     * 검색어 통계 기록
     *
     * 핵심 로직:
     * 1. 중복 검색 체크 (1분 내 동일 검색어 무시)
     * 2. Redis 점수 증가 (실시간 반영)
     * 3. DB 통계 업데이트 (영구 저장)
     *
     * 점수 계산 (명세서 요구사항):
     * - 기본 점수: searchCount
     * - 브랜드 키워드: +100 (BRAND 타입)
     * - 정확 매칭: +50 (추후 구현)
     *
     * 트랜잭션:
     * - @Transactional: DB 업데이트 원자성 보장
     * - Redis 실패 시에도 DB는 정상 저장
     *
     * @param keyword 검색어
     * @param userId 사용자 ID (중복 방지용)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = false)
    public void recordSearch(String keyword, UUID userId) {
        log.info("검색어 통계 기록: keyword={}, userId={}", keyword, userId);

        // 1. userId가 있을 때만 중복 검색 체크 (1분 내 동일 검색어 무시)
        if (userId != null && isDuplicateSearch(keyword, userId)) {
            log.info("중복 검색 무시: keyword={}, userId={}", keyword, userId);
            return;
        }

        // 2. Redis 실시간 업데이트
        try {
            updateRedisAutoComplete(keyword);
        } catch (Exception e) {
            log.error("Redis 업데이트 실패 (계속 진행): keyword={}", keyword, e);
            // Redis 실패해도 DB는 저장
        }

        // 3. DB 영구 저장
        updateDatabaseStatistics(keyword);

        // 4. userId가 있을 때만 중복 방지 키 설정 (1분 TTL)
        if (userId != null) {
            try {
                setDedupeKey(keyword, userId);
            } catch (Exception e) {
                log.warn("Redis 중복방지 키 설정 실패: keyword={}", keyword, e);
            }
        }
    }

    /**
     * 중복 검색 체크
     *
     * 동작:
     * - Redis Key: "search:dedupe:{keyword}:{userId}"
     * - 존재 여부 체크
     * - 존재하면 중복 (1분 내 재검색)
     *
     * 목적:
     * - 동일 사용자가 1분 내 같은 검색어 반복 → 통계 왜곡 방지
     * - 실수로 연속 클릭 방지
     *
     * @param keyword 검색어
     * @param userId 사용자 ID
     * @return true: 중복 검색, false: 신규 검색
     */
    private boolean isDuplicateSearch(String keyword, UUID userId) {
        try {
            String dedupeKey = SEARCH_DEDUPE_PREFIX + keyword + ":" + userId;
            Boolean hasKey = redisTemplate.hasKey(dedupeKey);
            return hasKey != null && hasKey;
        } catch (Exception e) {
            log.warn("Redis 중복 체크 실패, 통계 기록 계속 진행: keyword={}", keyword, e);
            return false;  // Redis 실패 시 중복 아님으로 처리
        }
    }

    /**
     * 중복 방지 키 설정 (실수/연타 방지)
     *
     * 동작:
     * - Redis Key 생성 (값은 "1")
     * - TTL 10초 설정
     * - 10초 후 자동 삭제
     *
     * @param keyword 검색어
     * @param userId 사용자 ID
     */
    private void setDedupeKey(String keyword, UUID userId) {
        try {
            String dedupeKey = SEARCH_DEDUPE_PREFIX + keyword + ":" + userId;
            redisTemplate.opsForValue().set(dedupeKey, "1", SEARCH_DEDUPE_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Redis 중복방지 키 설정 실패 (무시): keyword={}", keyword, e);
            // 필수 기능 아니므로 무시
        }
    }

    /**
     * Redis 자동완성 업데이트
     *
     * 동작:
     * 1. 기존 점수 조회
     * 2. 검색어 타입 확인 (BRAND 여부)
     * 3. 점수 계산: 기존점수 + 1 + 브랜드가중치
     * 4. Redis Sorted Set 업데이트
     *
     * 점수 계산 예시:
     * - "아이폰" (BRAND): 100 → 201 (+1 +100)
     * - "중고폰" (GENERAL): 50 → 51 (+1)
     *
     * Redis 명령어:
     * - ZINCRBY autocomplete:all 1 "아이폰"
     * - 또는 ZADD autocomplete:all {계산된점수} "아이폰"
     *
     * @param keyword 검색어
     */
    private void updateRedisAutoComplete(String keyword) {
        ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();

        // 1. 기존 점수 조회 (없으면 0)
        Double currentScore = zSetOps.score(AUTOCOMPLETE_KEY, keyword);
        if (currentScore == null) {
            currentScore = 0.0;
        }

        // 2. 브랜드 가중치 계산
        int brandWeight = isBrandKeyword(keyword) ? BRAND_WEIGHT : 0;

        // 3. 새 점수 계산 (검색 1회 + 브랜드 가중치)
        double newScore = currentScore + SEARCH_INCREMENT + brandWeight;

        // 4. Redis 업데이트
        zSetOps.add(AUTOCOMPLETE_KEY, keyword, newScore);

        log.debug("Redis 자동완성 업데이트: keyword={}, score={}", keyword, newScore);
    }

    /**
     * DB 통계 업데이트 (UPSERT)
     *
     * 개선 사항:
     * - PostgreSQL의 INSERT ... ON CONFLICT DO UPDATE 활용
     * - DB 레벨에서 원자적으로 처리 (동시성 이슈 해결)
     * - Race Condition 없음 (조회 → 수정 사이의 갭 제거)
     * - Lost Update 없음 (DB가 원자적으로 처리)
     *
     * 동작:
     * - 검색어가 없으면: INSERT (search_count = 1, keyword_type 자동 판단)
     * - 검색어가 있으면: UPDATE (search_count + 1, last_searched_at 갱신)
     *
     * @param keyword 검색어
     */
    private void updateDatabaseStatistics(String keyword) {
        String keywordType = SearchKeyword.determineKeywordType(keyword).name();

        searchKeywordRepository.upsertSearchCount(keyword, keywordType);
        log.debug("검색어 통계 업데이트 완료: keyword={}", keyword);
    }

    /**
     * 브랜드 키워드 판단
     *
     * 동작:
     * - 키워드에 브랜드명 포함 여부 체크
     *
     * 용도:
     * - Redis 점수 계산 시 브랜드 가중치 적용
     *
     * @param keyword 검색어
     * @return true: 브랜드 키워드, false: 일반 키워드
     */
    private boolean isBrandKeyword(String keyword) {
        return SearchKeyword.determineKeywordType(keyword) == KeywordType.BRAND;
    }

    /**
     * 인기 검색어 Top 10 조회 (Redis → DB Fallback)
     *
     * 동작:
     * 1. Redis에서 score 기준 상위 10개 조회 시도
     * 2. Redis 실패/데이터 없음 → DB Fallback
     * 3. 순위 정보 추가 (1~10)
     *
     * @return 인기 검색어 목록 (순위 포함)
     */
    public List<PopularKeywordResponse> getPopularKeywords() {
        log.debug("인기 검색어 조회");

        // 1. Redis에서 상위 10개 조회 시도
        try {
            ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();

            // 점수 역순으로 상위 10개 (0~9 인덱스)
            Set<ZSetOperations.TypedTuple<String>> results =
                    zSetOps.reverseRangeWithScores(
                            AUTOCOMPLETE_KEY,
                            0,
                            POPULAR_KEYWORDS_LIMIT - 1);

            if (results != null && !results.isEmpty()) {
                log.info("Redis에서 인기 검색어 조회 성공: {} 건", results.size());
                return convertRedisToPopularKeywordResponse(results);
            }
            log.debug("Redis 인기 검색어 결과 없음, DB Fallback 실행");

        } catch (Exception e) {
            log.error("Redis 인기 검색어 조회 실패, DB Fallback", e);
        }

        // 2. Redis 실패 시 DB Fallback
        return getFallbackPopularKeywords();
    }

    /**
     * Redis 결과 → Response 변환
     *
     * @param results Redis Sorted Set 결과
     * @return 인기 검색어 응답 목록
     */
    private List<PopularKeywordResponse> convertRedisToPopularKeywordResponse(
            Set<ZSetOperations.TypedTuple<String>> results) {
        int[] rank = {1};

        return results.stream()
                .map(tuple -> PopularKeywordResponse.ofRedis(
                        tuple.getValue(),
                        tuple.getScore(),
                        rank[0]++
                ))
                .toList();
    }

    /**
     * DB Fallback 인기 검색어 조회
     *
     * @return 인기 검색어 응답 목록
     */
    private List<PopularKeywordResponse> getFallbackPopularKeywords() {
        log.debug("DB에서 인기 검색어 조회");

        List<SearchKeyword> keywords = searchKeywordRepository.findTopBySearchCount(POPULAR_KEYWORDS_LIMIT);

        // 순위 정보 추가 (1부터 시작)
        return IntStream.range(0, keywords.size())
                .mapToObj(i -> PopularKeywordResponse.of(keywords.get(i), i + 1))
                .toList();
    }
}
