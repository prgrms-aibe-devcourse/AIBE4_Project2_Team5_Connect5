package kr.eolmago.controller.api.search;

import io.swagger.v3.oas.annotations.tags.Tag;
import kr.eolmago.dto.api.search.response.AutocompleteResponse;
import kr.eolmago.dto.api.search.response.PopularKeywordResponse;
import kr.eolmago.service.search.SearchKeywordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Tag(name = "Search", description = "검색 부가 기능API")
@Slf4j
@RequiredArgsConstructor
public class SearchApiController {

    private final SearchKeywordService searchKeywordService;

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
