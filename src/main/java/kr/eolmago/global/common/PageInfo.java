package kr.eolmago.global.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

/**
 * 페이지네이션 공통 정보
 * 팀원 누구나 사용 가능
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageInfo {

    private int currentPage;      // 현재 페이지 (0-based)
    private int totalPages;       // 전체 페이지 수
    private long totalElements;   // 전체 데이터 수
    private int size;             // 페이지 크기
    private boolean first;        // 첫 페이지 여부
    private boolean last;         // 마지막 페이지 여부
    private boolean hasNext;      // 다음 페이지 존재 여부
    private boolean hasPrevious;  // 이전 페이지 존재 여부

    // Spring Data Page로부터 생성
    public static PageInfo from(Page<?> page) {
        return PageInfo.builder()
                .currentPage(page.getNumber())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .size(page.getSize())
                .first(page.isFirst())
                .last(page.isLast())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }

    /**
     * 페이지 번호 목록 생성 (UI용)
     * 예: 현재 5페이지 → [1, 2, 3, 5, 5]
     */
    public int[] getPageNumbers() {
        int start = Math.max(0, currentPage - 2);
        int end = Math.min(totalPages - 1, currentPage + 2);

        int[] pages = new int[end - start + 1];
        for (int i = 0; i < pages.length; i++) {
            pages[i] = start + i;
        }
        return pages;
    }
}
