package kr.eolmago.service.auction;

import kr.eolmago.domain.entity.auction.enums.AuctionStatus;
import kr.eolmago.domain.entity.auction.enums.ItemCategory;
import kr.eolmago.dto.api.auction.response.AuctionListDto;
import kr.eolmago.dto.api.auction.response.AuctionListResponse;
import kr.eolmago.dto.api.common.PageResponse;
import kr.eolmago.global.util.ChosungUtils;
import kr.eolmago.repository.auction.AuctionRepository;
import kr.eolmago.repository.auction.AuctionSearchRepository;
import kr.eolmago.service.search.SearchKeywordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 경매 통합 검색 Service
 *
 * 역할:
 * - 키워드 타입 자동 감지 (초성/한글/기타)
 * - 검색 전략 선택 (Full-Text → Trigram Fallback)
 * - 필터링 (카테고리, 브랜드, 가격 범위)
 * - 정렬 (최신순, 인기순, 마감임박순, 가격순)
 * - 검색어 통계 기록 연동
 * - 추천 키워드 제공 (결과 없을 때)
 *
 * 검색 전략 (폴백 체인):
 * 1. 키워드 타입 판단 (ChosungUtils)
 * 2. 초성: searchByChosung()
 * 3. 일반: searchByFullText()
 * 4. 결과 없으면: searchByTrigram() (오타 교정)
 * 5. 여전히 없으면: 빈 결과 (Controller에서 추천 키워드)
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuctionSearchService {

    private final AuctionSearchRepository auctionSearchRepository;
    private final AuctionRepository auctionRepository;
    private final SearchKeywordService searchKeywordService;

    /**
     * 통합 검색 (필터 + 정렬)
     *
     * 검색 흐름:
     * 1. 키워드 검증 (빈 값 체크)
     * 2. 키워드 타입 판단
     *    - CHOSUNG: "ㅇㅍ" → 초성 검색
     *    - HANGUL: "아이폰" → Full-Text 검색
     *    - OTHER: "iPhone" → Full-Text 검색
     *
     * 3. 타입별 검색 실행
     *    - 초성: searchByChosung()
     *    - 일반: searchByFullTextWithFallback()
     *
     * 4. 결과 처리
     *    - 있으면: 검색어 통계 기록
     *    - 없으면: 빈 결과 (통계 기록 안 함)
     *
     * @param keyword 검색 키워드
     * @param category 카테고리
     * @param brands 브랜드
     * @param minPrice 최소 가격
     * @param maxPrice 최대 가격
     * @param sort 정렬 옵션
     * @param status 경매 상태
     * @param pageable 페이지 정보
     * @param userId 사용자 ID (통계 기록용, null 가능)
     * @return 검색 결과
     */
    public PageResponse<AuctionListResponse> search(
            String keyword,
            ItemCategory category,
            List<String> brands,
            Integer minPrice,
            Integer maxPrice,
            String sort,
            AuctionStatus status,
            Pageable pageable,
            UUID userId
    ) {
        log.info("통합 검색 시작: keyword={}, category={}, minPrice={}, maxPrice={}, sort={}, status={}, page={}, userId={}", keyword, category, minPrice, maxPrice, sort, status, pageable.getPageNumber(), userId);

        // 1. 키워드 검증 및 처리
        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
        String trimmedKeyword = hasKeyword ? keyword.trim() : null;

        // 2. 검색 실행
        PageResponse<AuctionListResponse> result;

        String keywordType = ChosungUtils.getKeywordType(keyword);
        if (hasKeyword) {
            log.info("키워드 타입: {}", keywordType);

            // 타입별 검색 실행
            result = switch (keywordType) {
                case "CHOSUNG" -> {
                    log.debug("초성 검색 실행");
                    yield searchByChosung(trimmedKeyword, category, brands, minPrice, maxPrice, sort, status, pageable);
                }
                case "HANGUL", "MIXED", "OTHER" -> {
                    log.debug("Full-Text 검색 실행");
                    yield searchByFullTextWithFallback(trimmedKeyword, category, brands, minPrice, maxPrice, sort,status, pageable);
                }
                default -> {
                    log.warn("알 수 없는 키워드 타입: {}", keywordType);
                    yield PageResponse.of(Page.empty());
                }
            };
        } else {
            // 전체 조회 (키워드 없음) 그치만 검색된 상태에서 필터링을 할 수 있음으로 이런식으로 분기처리
            log.info("전체 조회 모드");
            Page<AuctionListDto> dtoPage = auctionRepository.searchList(
                    pageable,
                    sort,
                    status,
                    null,  // sellerId -> null로 변경하여 전체 조회
                    category,
                    brands,
                    minPrice,
                    maxPrice
            );
            result = PageResponse.of(dtoPage, AuctionListResponse::from);
        }

        // 3. 검색 결과가 있으면 통계 기록 (키워드만)
        if (result.pageInfo().totalElements() > 0 && !keywordType.equals("CHOSUNG")) {
            try {
                searchKeywordService.recordSearch(trimmedKeyword, userId);
                log.debug("검색어 통계 기록 완료: keyword={}, userId={}", keyword, userId);
            } catch (Exception e) {
                // 통계 기록 실패해도 검색 결과는 반환
                log.error("검색어 통계 기록 실패: keyword={}", trimmedKeyword, e);
            }
        }

        log.info("통합 검색 완료: keyword={}, results={}", keyword, result.pageInfo().totalElements());
        return result;
    }

    /**
     * 추천 키워드 조회 (검색 결과 없을 때)
     *
     * 동작:
     * - Repository에서 인기 검색어 기반 추천
     * - 최대 5개 반환
     *
     * 사용 시나리오:
     * - Controller에서 결과 없을 때 호출
     * - "혹시 이런 키워드를 찾으셨나요?" 표시
     *
     * @return 추천 키워드 목록 (최대 5개)
     */
    public List<String> getSuggestedKeywords() {
        log.debug("추천 키워드 조회");
        return auctionSearchRepository.getSuggestedKeywords();
    }

    // ============================================
    // 헬퍼 메서드
    // ============================================

    /**
     * 초성 검색 실행
     *
     * 동작:
     * 1. Repository에서 DTO 프로젝션 조회
     * 2. 결과 있으면 Response 변환 후 반환
     * 3. 결과 없으면 Full-Text로 폴백
     *
     * 데이터 흐름:
     * - Repository: Page<AuctionListDto> (썸네일, 닉네임 조립됨)
     * - Service: PageResponse<AuctionListResponse>
     *
     * 예시:
     * - "ㅇㅇㅍ" 검색
     *   → extract_chosung("아이폰 14") = "ㅇㅇㅍ 14"
     *   → LIKE 'ㅇㅇㅍ%' 매칭 ✅
     *
     * - "ㅇㅍ" 검색
     *   → 정확한 초성 순서 필요
     *   → 매칭 실패 → Full-Text 폴백
     *
     * @param keyword 초성 키워드
     * @param status 경매 상태
     * @param pageable 페이지 정보
     * @return 검색 결과
     */
    private PageResponse<AuctionListResponse> searchByChosung(
            String keyword,
            ItemCategory category,
            List<String> brands,
            Integer minPrice,
            Integer maxPrice,
            String sort,
            AuctionStatus status,
            Pageable pageable
    ) {
        log.debug("초성 검색 실행: keyword={}", keyword);

        // 1. 초성 패턴 (LIKE 'ㅇㅇㅍ%')
        String chosungPattern = keyword + "%";

        // 2. 초성 검색 (Native Query)
        Page<AuctionListDto> dtoPage = auctionSearchRepository.searchByChosung(
                chosungPattern,
                category, brands, minPrice, maxPrice, sort,
                status != null ? AuctionStatus.valueOf(status.name()) : null,
                pageable
        );

        // 2. 결과가 있으면 Response 변환 후 반환
        if (dtoPage.hasContent()) {
            log.debug("초성 검색 성공: {} 건", dtoPage.getTotalElements());
            return PageResponse.of(dtoPage, AuctionListResponse::from);
        }

        // 3. 결과 없으면 Full-Text로 폴백
        log.debug("초성 검색 결과 없음, Full-Text로 폴백");
        return searchByFullTextWithFallback(keyword, category, brands, minPrice, maxPrice, sort, status, pageable);
    }

    /**
     * Full-Text 검색 + Trigram 폴백
     *
     * 동작:
     * 1. Full-Text Search 실행 (Repository DTO 프로젝션)
     * 2. 결과 없으면 Trigram으로 폴백 (오타 교정)
     * 3. 여전히 없으면 빈 결과 반환
     *
     * 데이터 흐름:
     * - Repository: Page<AuctionListDto> (DTO 프로젝션)
     * - Service: DTO → Response 변환
     *
     * 예시:
     * - "아이폰" 검색
     *   → Full-Text 매칭 ✅
     *   → 결과 반환
     *
     * - "아이혼" 검색 (오타)
     *   → Full-Text 매칭 실패
     *   → Trigram 유사도 검색
     *   → similarity("아이폰", "아이혼") = 0.6
     *   → "아이폰" 결과 반환 ✅
     *
     * - "존재하지않는키워드" 검색
     *   → Full-Text 실패
     *   → Trigram 실패
     *   → 빈 결과 (Controller에서 추천 키워드 제공)
     *
     * @param keyword 검색 키워드
     * @param status 경매 상태
     * @param pageable 페이지 정보
     * @return 검색 결과
     */
    private PageResponse<AuctionListResponse> searchByFullTextWithFallback(
            String keyword,
            ItemCategory category,
            List<String> brands,
            Integer minPrice,
            Integer maxPrice,
            String sort,
            AuctionStatus status,
            Pageable pageable
    ) {
        log.info("Full-Text 검색 실행: keyword={}, status={}, pageable={} ", keyword, status, pageable);

        // 1. 키워드 전처리 (공백을 & 로 변환)
        String processedKeyword = keyword.trim()
                .replaceAll("[,.:;!?]", " ")
                .replaceAll("\\s+", " & ");

        // 2. Full-Text Search (Native Query)
        Page<AuctionListDto> dtoPage = auctionSearchRepository.searchByFullText(
                processedKeyword,
                category, brands, minPrice, maxPrice, sort,
                status,
                pageable
        );

        // 2. 결과가 있으면 Response 변환 후 반환
        if (dtoPage.hasContent()) {
            log.debug("Full-Text 검색 성공: {} 건", dtoPage.getTotalElements());
            return PageResponse.of(dtoPage, AuctionListResponse::from);
        }

        // 3. 결과 없으면 Trigram으로 폴백 (오타 교정)
        log.debug("Full-Text 검색 결과 없음, Trigram으로 폴백");
        dtoPage = auctionSearchRepository.searchByTrigram(
                processedKeyword,
                getDynamicThreshold(processedKeyword),
                category, brands, minPrice, maxPrice, sort,
                status,
                pageable
        );

        // 4. Trigram 결과 반환 (없어도 빈 Page)
        if (dtoPage.hasContent()) {
            log.debug("Trigram 검색 성공: {} 건", dtoPage.getTotalElements());
        } else {
            log.debug("모든 검색 전략 실패");
        }

        return PageResponse.of(dtoPage, AuctionListResponse::from);
    }

    /**
     * 키워드 길이에 따라 동적으로 threshold 조정
     */
    private double getDynamicThreshold(String keyword) {
        int length = keyword.length();
        System.out.println("length: " + length);

        if (length <= 2) {
            return 0.5;  // 짧은 키워드는 엄격하게
        } else if (length <= 4) {
            return 0.3;  // 중간 길이
        } else {
            return 0.25; // 긴 키워드는 관대하게
        }
    }


}

