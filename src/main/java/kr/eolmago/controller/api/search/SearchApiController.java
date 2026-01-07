package kr.eolmago.controller.api.search;

import kr.eolmago.domain.entity.auction.enums.AuctionStatus;
import kr.eolmago.dto.api.auction.response.AuctionListResponse;
import kr.eolmago.dto.api.common.PageResponse;
import kr.eolmago.dto.api.search.response.AutocompleteResponse;
import kr.eolmago.dto.api.search.response.PopularKeywordResponse;
import kr.eolmago.service.auction.AuctionSearchService;
import kr.eolmago.service.search.SearchKeywordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 검색 REST API Controller
 *
 * 역할:
 * - 자동완성 API 제공
 * - 인기 검색어 API 제공
 * - 통합 검색
 * - 초성으로 검색
 *
 */
@RestController
@RequestMapping("/api/search")
@Slf4j
@RequiredArgsConstructor
public class SearchApiController {

    private final SearchKeywordService searchKeywordService;
    private final AuctionSearchService auctionSearchService;

    /**
     * 경매 검색 API
     *
     * 사용 예시:
     * ```
     * GET /api/search?keyword=아이폰&status=LIVE&page=0&size=20
     * GET /api/search?keyword=ㅇㅍ&page=0
     * GET /api/search?keyword=아이혼&page=0  (오타 교정)
     * ```
     *
     * 검색 전략:
     * 1. 초성: "ㅇㅇㅍ" → 초성 검색
     * 2. 일반: "아이폰" → Full-Text 검색
     * 3. 오타: "아이혼" → Trigram 유사도 검색
     * 4. 실패: 추천 키워드 제공
     *
     * @param keyword 검색 키워드 (필수)
     * @param status 경매 상태 (선택, 기본: 전체)
     * @param page 페이지 번호 (0-based, 기본: 0)
     * @param size 페이지 크기 (기본: 20)
     * @param userId 사용자 ID (선택, 통계 기록용)
     * @return 검색 결과 + 추천 키워드 (결과 없을 때)
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> search(
            @RequestParam String keyword,
            @RequestParam(required = false) AuctionStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) UUID userId
    ) {
        log.info("검색 API 호출: keyword={}, status={}, page={}", keyword, status, page);

        // 1. 페이지 정보 생성
        Pageable pageable = PageRequest.of(page, size);

        // 2. 검색 실행 (Service에서 통합 검색)
        PageResponse<AuctionListResponse> results = auctionSearchService.search(
                keyword,
                status,
                pageable,
                userId
        );

        // 3. 응답 구성
        Map<String, Object> response = new HashMap<>();
        response.put("results", results);

        // 4. 검색 결과 없으면 추천 키워드 제공
        if (results.pageInfo().totalElements() == 0) {
            List<String> suggestions = auctionSearchService.getSuggestedKeywords();
            response.put("suggestions", suggestions);
            log.debug("검색 결과 없음, 추천 키워드 제공: {}", suggestions);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * 자동완성 API
     *
     * 동작:
     * 1. 사용자가 검색어 입력 중
     * 2. 매 타이핑마다 이 API 호출
     * 3. Redis에서 실시간 자동완성 후보 반환
     *
     * 성능:
     * - Redis 기반: < 100ms
     * - Redis 실패 시 DB Fallback: < 500ms
     *
     * @param query 검색어 앞부분 (예: "아이", "갤럭")
     * @return 자동완성 후보 목록 (최대 10개)
     */
    @GetMapping("/autocomplete")
    public ResponseEntity<List<AutocompleteResponse>> autocomplete(
            @RequestParam("q") String query
    ) {
        log.info("자동완성 API 호출: query={}", query);

        // 빈 문자열 체크
        if (query == null || query.trim().isEmpty()) {
            log.warn("자동완성 쿼리 비어있음");
            return ResponseEntity.ok(List.of());
        }

        // 너무 짧은 검색어 필터링 (성능 최적화)
        if (query.trim().length() < 1) {
            log.debug("자동완성 쿼리 너무 짧음: length={}", query.length());
            return ResponseEntity.ok(List.of());
        }

        // 자동완성 Service 호출(초성 자동 판단)
        List<AutocompleteResponse> results = searchKeywordService.getAutoComplete(query.trim());

        log.info("자동완성 결과: query={}, count={}", query, results.size());
        return ResponseEntity.ok(results);
    }

    /**
     * 인기 검색어 Top 10 API
     *
     * 동작:
     * - DB에서 검색량 기준 상위 10개 조회
     * - 순위 정보 포함하여 반환
     *
     * 호출 시점:
     * - 메인 페이지 로드 시
     * - 검색 페이지 우측 위젯
     * - 주기적 갱신 (5분마다)
     *
     * 캐싱 고려:
     * - 현재: 매 요청마다 DB 조회
     * - 향후: Redis 캐싱 (5분 TTL) 추가 가능
     *
     * @return 인기 검색어 목록 (순위 포함, 최대 10개)
     */
    @GetMapping("/popular")
    public ResponseEntity<List<PopularKeywordResponse>> getPopularKeywords() {
        log.info("인기 검색어 API 호출");

        List<PopularKeywordResponse> results = searchKeywordService.getPopularKeywords();

        log.info("인기 검색어 결과: count={}", results.size());

        return ResponseEntity.ok(results);
    }

    /**
     * 검색어 통계 기록 API (테스트/디버깅용)
     *  - 주의: 이 API는 직접 호출하지 않음!
     *
     * 동작:
     * 1. 검색 실행 → 이 메서드 호출
     * 2. Redis + DB 통계 업데이트
     * 3. 자동완성 점수 증가
     *
     * 테스트 용도로만 외부 노출:
     * - 실제 서비스에선 내부 메서드로 전환 권장
     * - 또는 인증 추가 필요
     *
     * @param keyword 검색어
     * @param userId 사용자 ID (세션/토큰에서 추출)
     * @return 성공 응답
     */
    @PostMapping("/record")
    public ResponseEntity<Map<String, String>> recordSearch(
            @RequestParam String keyword,
            @RequestParam UUID userId
    ) {
        log.info("검색어 통계 기록 API 호출: keyword={}, userId={}", keyword, userId);

        // 검색어 통계 기록
        searchKeywordService.recordSearch(keyword, userId);

        Map<String, String> response = new HashMap<>();
        response.put("message", "검색어 통계 기록 완료");
        response.put("keyword", keyword);

        return ResponseEntity.ok(response);
    }
}
