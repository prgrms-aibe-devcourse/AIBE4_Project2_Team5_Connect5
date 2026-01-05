package kr.eolmago.global.util;

import kr.eolmago.dto.api.common.PageInfo;

/**
 * 페이지네이션 유틸리티
 *
 * 역할:
 * - 페이지네이션 관련 계산 로직
 * - DTO에서 분리된 비즈니스 로직
 *
 * 팀원 사용 가이드:
 * - 프론트엔드 페이지 번호 배열 생성 시 사용
 * - Thymeleaf 템플릿에서 페이지 번호 표시 시 사용
 */
public class PageUtils {
    /**
     * UI용 페이지 번호 배열 생성 (1-based)
     *
     * 동작:
     * - 현재 페이지 기준 앞뒤 2개씩 표시
     * - 총 최대 5개 페이지 번호 표시
     * - 사용자에게 보여지는 번호 (1, 2, 3...)
     *
     * 예시:
     * - 현재 페이지 0 (첫 페이지): [1, 2, 3]
     * - 현재 페이지 5: [4, 5, 6, 7, 8]
     * - 현재 페이지 9 (마지막, 총 10페이지): [8, 9, 10]
     *
     * 실제 사례:
     * - 총 데이터 18개, 페이지당 8개
     * - 총 페이지: 3 (0, 1, 2)
     * - 표시: [1, 2, 3]
     *
     * 사용 예시:
     * // Service
     *  PageInfo pageInfo = PageInfo.from(page);
     *  int[] pageNumbers = PageUtils.getPageNumbers(pageInfo);
     * // 결과: [1, 2, 3] (사용자 표시용)
     *
     * // Controller (Thymeleaf)
     *  model.addAttribute("pageInfo", pageInfo);
     *  model.addAttribute("pageNumbers", PageUtils.getPageNumbers(pageInfo));
     *
     * Thymeleaf 사용:
     * ```html
     * <ul class="pagination">
     *   <li th:each="pageNum : ${pageNumbers}"
     *       th:classappend="${pageNum - 1 == pageInfo.currentPage} ? 'active'">
     *     <!-- 클릭 시 0-based로 전환 (pageNum - 1) -->
     *     <a th:href="@{/auctions(page=${pageNum - 1})}"
     *        th:text="${pageNum}">1</a>
     *   </li>
     * </ul>
     * ```
     *
     * @param pageInfo 페이지 정보 (내부적으로 0-based)
     * @return 페이지 번호 배열 (사용자 표시용 1-based)
     */
    public static int[] getPageNumbers(PageInfo pageInfo) {
        // 현재 페이지 기준 앞뒤 2개씩 (0-based 계산)
        int start = Math.max(0, pageInfo.currentPage() - 2);
        int end = Math.min(pageInfo.totalPages() - 1, pageInfo.currentPage() + 2);

        // 배열 생성 (1-based로 변환)
        int[] pageNumbers = new int[end - start + 1];
        for (int i = 0; i < pageNumbers.length; i++) {
            pageNumbers[i] = start + i + 1;  // (1-based)
        }

        return pageNumbers;
    }

    /**
     * 커스텀 범위 페이지 번호 생성 (1-based)
     *
     * 동작:
     * - 현재 페이지 기준 앞뒤 range개씩 표시
     *
     * 예시:
     * - getPageNumbers(pageInfo, 3): 앞뒤 3개씩, 최대 7개 (1-based)
     * - getPageNumbers(pageInfo, 1): 앞뒤 1개씩, 최대 3개 (1-based)
     *
     * @param pageInfo 페이지 정보
     * @param range 앞뒤 표시 개수
     * @return 페이지 번호 배열 (1-based)
     */
    public static int[] getPageNumbers(PageInfo pageInfo, int range) {
        int start = Math.max(0, pageInfo.currentPage() - range);
        int end = Math.min(pageInfo.totalPages() - 1, pageInfo.currentPage() + range);

        int[] pageNumbers = new int[end - start + 1];
        for (int i = 0; i < pageNumbers.length; i++) {
            pageNumbers[i] = start + i + 1;  // 1-based
        }

        return pageNumbers;
    }
}
