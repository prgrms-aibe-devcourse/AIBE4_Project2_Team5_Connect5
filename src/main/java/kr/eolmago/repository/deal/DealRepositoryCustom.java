package kr.eolmago.repository.deal;

import kr.eolmago.domain.entity.deal.Deal;
import kr.eolmago.dto.api.deal.response.DealDetailDto;
import kr.eolmago.dto.api.deal.response.DealPdfDto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Deal Custom Repository
 */
public interface DealRepositoryCustom {

    // 거래 상세 조회
    Optional<DealDetailDto> findDetailById(Long dealId);

    /**
     * PDF 생성을 위한 Deal 정보 조회
     *
     * Deal + Auction + AuctionItem + User + UserProfile + SocialLogin을 조인하여
     * 한 번의 쿼리로 필요한 모든 데이터 조회
     *
     * @param dealId 거래 ID
     * @return PDF 생성용 DTO
     */
    Optional<DealPdfDto> findPdfDataByDealId(Long dealId);

    /**
     * 자동 완료 가능한 거래 조회
     *
     * 조건:
     * 1. CONFIRMED 상태
     * 2. shippedAt이 threshold 이전 (배송 시작 후 N일 경과)
     * 3. 해당 auction에 진행 중인 신고가 없음 (PENDING, UNDER_REVIEW 상태의 Report 없음)
     *
     * @param shippedAtThreshold 배송 시작 기준 시간 (이 시간보다 이전에 배송된 거래)
     * @return 자동 완료 가능한 거래 목록
     */
    List<Deal> findCompletableDeals(OffsetDateTime shippedAtThreshold);
}
