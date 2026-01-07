package kr.eolmago.domain.entity.search;

import jakarta.persistence.*;
import kr.eolmago.domain.entity.common.CreatedAtEntity;
import kr.eolmago.domain.entity.search.enums.KeywordType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * 검색 키워드 엔티티
 *
 * 역할:
 * - 검색어 통계 저장 (검색량, 마지막 검색 시간)
 * - Redis 캐시 백업용 영구 저장소
 * - 검색어 타입 분류 (브랜드/모델/일반)
 *
 * 연결 부분:
 * - Redis: 실시간 자동완성 캐시의 백업 저장소
 * - 자동완성 API: 검색어 통계 수집 → 햔재 테이블에 저장
 * - 인기 검색어: 이 테이블에서 search_count 기준 조회
 */
@Entity
@Table(
        name = "search_keywords",
        indexes = {
                @Index(name = "idx_search_keywords_keyword", columnList = "keyword", unique = true),
                @Index(name = "idx_search_count", columnList = "search_count DESC"),
                @Index(name = "idx_keyword_type", columnList = "keyword_type, search_count DESC")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SearchKeyword extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String keyword;

    @Column(name = "search_count", nullable = false)
    private Integer searchCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "keyword_type", length = 20, nullable = false)
    private KeywordType keywordType;

    /**
     * 마지막 검색 시간
     *
     * 용도:
     * - 오래된 검색어 정리 (배치 작업)
     * - 최근 검색 트렌드 분석
     *
     * 참고: updated_at 대신 이 컬럼 사용
     * - updated_at: 모든 필드 변경 시 업데이트
     * - lastSearchedAt: 검색 발생 시점만 기록 (의미 명확)
     */
    @Column(name = "last_searched_at", nullable = false)
    private OffsetDateTime lastSearchedAt;

    // 초성 컬럼 (Generated Column)
    @Column(name = "keyword_chosung", insertable = false, updatable = false)
    private String keywordChosung;  // 읽기 전용


    public static SearchKeyword create(String keyword) {
        SearchKeyword searchKeyword = new SearchKeyword();
        searchKeyword.keyword = keyword;
        searchKeyword.searchCount = 1;
        searchKeyword.keywordType = determineKeywordType(keyword);
        searchKeyword.lastSearchedAt = OffsetDateTime.now();
        return searchKeyword;
    }

    /**
     * 검색 횟수 증가
     * lastSearchedAt도 현재 시간으로 업데이트
     */
    public void incrementSearchCount() {
        this.searchCount++;
        this.lastSearchedAt = OffsetDateTime.now();
    }

    /**
     * 키워드 타입 자동 결정
     *
     * @param keyword 검색어
     * @return KeywordType (BRAND, MODEL, GENERAL)
     */
    private static KeywordType determineKeywordType(String keyword) {
        String lowerKeyword = keyword.toLowerCase();

        // 브랜드 키워드 (명세서에서 브랜드 가중치 +100 적용)
        if (lowerKeyword.matches(".*(아이폰|갤럭시|픽셀|샤오미|apple|samsung|google|xiaomi).*")) {
            return KeywordType.BRAND;
        }

        // 모델명 키워드 (숫자 포함)
        if (lowerKeyword.matches(".*\\d+.*")) {
            return KeywordType.MODEL;
        }

        return KeywordType.GENERAL;
    }

}
