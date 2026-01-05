package kr.eolmago.dto.api.common;

import org.springframework.data.domain.Page;

/**
 * 페이지네이션 공통 정보
 *
 * 역할:
 *  - 페이지네이션 메타 정보 제공
 *  - 팀 전체 공용 DTO
 *
 *  팀원 사용 가이드:
 *  - Service에서 PageInfo.from(page) 호출
 *  - Controller에서 PageResponse에 포함하여 반환
 *  - 페이지 번호 배열 필요 시 PageUtils.getPageNumbers() 사용
 */
public record PageInfo(
        int currentPage,      // 현재 페이지 (1-based)
        int totalPages,       // 전체 페이지 수
        long totalElements,   // 전체 데이터 수
        int size,             // 페이지 크기
        boolean first,        // 첫 페이지 여부
        boolean last,         // 마지막 페이지 여부
        boolean hasNext,      // 다음 페이지 존재 여부
        boolean hasPrevious  // 이전 페이지 존재 여부
) {

    // Spring Data Page로부터 생성
    public static PageInfo from(Page<?> page) {
        return new PageInfo(
                page.getNumber(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.getSize(),
                page.isFirst(),
                page.isLast(),
                page.hasNext(),
                page.hasPrevious()
        );
    }
}
