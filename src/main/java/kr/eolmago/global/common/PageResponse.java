package kr.eolmago.global.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * REST API 페이지네이션 응답
 * 팀원 누구나 사용 가능
 *
 * 사용 예시:
 * PageResponse<AuctionListResponse> response = PageResponse.of(auctionPage);
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageResponse<T> {

    private List<T> content;      // 실제 데이터
    private PageInfo pageInfo;    // 페이지 정보

    // Spring Data Page + Converter로부터 생성
    public static <T, R> PageResponse<R> of(Page<T> page, Function<T, R> converter) {
        List<R> content = page.getContent().stream()
                .map(converter)
                .toList();

        return PageResponse.<R>builder()
                .content(content)
                .pageInfo(PageInfo.from(page))
                .build();
    }

    // 이미 변환된 Page<DTO>인 경우
    public static <T> PageResponse<T> of(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .pageInfo(PageInfo.from(page))
                .build();
    }
}