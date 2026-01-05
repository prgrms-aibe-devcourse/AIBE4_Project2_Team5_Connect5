package kr.eolmago.dto.api.common;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * REST API 페이지네이션 응답
 *
 * 역할:
 *  - 페이지 데이터 + 메타 정보 통합 응답
 *  - 팀 전체 공용 DTO
 *  - 일관된 페이지네이션 응답 형식 제공
 *
 * 팀원 사용 가이드:
 *
 * 1. Entity → DTO 변환 필요한 경우:
 *  Page<Auction> auctionPage = repository.findAll(pageable);
 *  PageResponse<AuctionListResponse> response =
 *     PageResponse.of(auctionPage, AuctionListResponse::from);
 *
 * 2. 이미 DTO인 경우:
 *  Page<AuctionDto> dtoPage = repository.findAllDto(pageable);
 *  PageResponse<AuctionDto> response = PageResponse.of(dtoPage);
 */
public record PageResponse<T> (
        List<T> content,    // 실제 데이터 목록
        PageInfo pageInfo   // 페이지 메타 정보
){
    /**
     * Spring Data Page + 변환 함수로 PageResponse 생성
     *
     * 사용 시점:
     * - Entity → DTO 변환이 필요한 경우
     * - Repository에서 Page<Entity> 반환 시
     *
     * 사용 예시:
     * // Service
     *  Page<Auction> auctionPage = auctionRepository.findByStatus(
     *      AuctionStatus.LIVE,
     *      pageable
     *  );
     *  return PageResponse.of(auctionPage, AuctionListResponse::from);
     */
    public static <T, R> PageResponse<R> of(Page<T> page, Function<T, R> converter) {
        List<R> content = page.getContent()
                .stream()
                .map(converter)
                .toList();

        return new PageResponse<>(
                content,
                PageInfo.from(page)
        );
    }

    /**
     * 이미 변환된 Page<DTO>로 PageResponse 생성
     *
     * 사용 시점:
     * - Repository에서 이미 DTO로 조회한 경우
     * - QueryDSL Projection으로 DTO 직접 조회 시
     */
    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                PageInfo.from(page)
        );
    }
}