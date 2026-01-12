package kr.eolmago.service.deal;

import kr.eolmago.domain.entity.deal.Deal;
import kr.eolmago.dto.api.deal.response.BuyerDealDetailResponse;
import kr.eolmago.global.exception.BusinessException;
import kr.eolmago.global.exception.ErrorCode;
import kr.eolmago.repository.deal.DealRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 구매자 거래 상세 조회 / 확정 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BuyerDealDetailService {

    private final DealRepository dealRepository;

    /**
     * 구매자의 특정 거래 상세 조회 (권한 검증)
     */
    public BuyerDealDetailResponse getDealDetail(Long dealId, UUID buyerId) {
        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEAL_NOT_FOUND));

        // 권한 검증: 내가 구매자인 거래인지 확인
        if (!deal.isBuyer(buyerId)) {
            throw new BusinessException(ErrorCode.DEAL_UNAUTHORIZED);
        }

        return toDetailResponse(deal);
    }

    /**
     * 구매자 확정 처리
     */
    @Transactional
    public void confirmByBuyer(Long dealId, UUID buyerId) {
        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEAL_NOT_FOUND));

        // 권한 검증: 내가 구매자인 거래인지 확인
        if (!deal.isBuyer(buyerId)) {
            throw new BusinessException(ErrorCode.DEAL_UNAUTHORIZED);
        }

        // 이미 구매자 확정이 된 경우 예외
        if (deal.getBuyerConfirmedAt() != null) {
            throw new BusinessException(ErrorCode.DEAL_ALREADY_CONFIRMED);
        }

        deal.confirmByBuyer();
    }

    /**
     * 엔티티 → 상세 응답 DTO 변환
     */
    private BuyerDealDetailResponse toDetailResponse(Deal deal) {
        String sellerNickname = "판매자";
        if (deal.getSeller() != null && deal.getSeller().getUserProfile() != null) {
            sellerNickname = deal.getSeller().getUserProfile().getNickname();
        }

        return new BuyerDealDetailResponse(
                deal.getDealId(),
                deal.getFinalPrice(),
                // DealStatus(enum) -> String
                deal.getStatus().name(),
                // OffsetDateTime -> String (null 체크 포함)
                deal.getCreatedAt() != null ? deal.getCreatedAt().toString() : null,
                // 확정 여부 boolean
                deal.getSellerConfirmedAt() != null,
                deal.getBuyerConfirmedAt() != null,
                // 각 시간들도 String 필드에 맞게 변환
                deal.getSellerConfirmedAt() != null ? deal.getSellerConfirmedAt().toString() : null,
                deal.getBuyerConfirmedAt() != null ? deal.getBuyerConfirmedAt().toString() : null,
                deal.getConfirmedAt() != null ? deal.getConfirmedAt().toString() : null,
                deal.getConfirmByAt() != null ? deal.getConfirmByAt().toString() : null,
                deal.getShipByAt() != null ? deal.getShipByAt().toString() : null,
                deal.getCompletedAt() != null ? deal.getCompletedAt().toString() : null,
                deal.getSeller() != null ? deal.getSeller().getUserId() : null,
                sellerNickname
        );
    }
}
