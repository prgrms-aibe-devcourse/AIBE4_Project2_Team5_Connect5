package kr.eolmago.service.deal;

import kr.eolmago.domain.entity.deal.Deal;
import kr.eolmago.dto.api.deal.response.SellerDealDetailResponse;
import kr.eolmago.global.exception.BusinessException;
import kr.eolmago.global.exception.ErrorCode;
import kr.eolmago.repository.deal.DealRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 판매자 거래 상세 조회 / 확정 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerDealDetailService {

    private final DealRepository dealRepository;

    /**
     * 판매자의 특정 거래 상세 조회 (권한 검증)
     */
    public SellerDealDetailResponse getDealDetail(Long dealId, UUID sellerId) {
        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEAL_NOT_FOUND));

        // 권한 검증: 내가 판매자인 거래인지 확인
        if (!deal.isSeller(sellerId)) {
            throw new BusinessException(ErrorCode.DEAL_UNAUTHORIZED);
        }

        return toDetailResponse(deal);
    }

    /**
     * 판매자 확정 처리
     */
    @Transactional
    public void confirmBySeller(Long dealId, UUID sellerId) {
        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEAL_NOT_FOUND));

        // 권한 검증: 내가 판매자인 거래인지 확인
        if (!deal.isSeller(sellerId)) {
            throw new BusinessException(ErrorCode.DEAL_UNAUTHORIZED);
        }

        // 이미 판매자 확정이 된 경우 예외
        if (deal.getSellerConfirmedAt() != null) {
            throw new BusinessException(ErrorCode.DEAL_ALREADY_CONFIRMED);
        }

        deal.confirmBySeller();
    }

    /**
     * 엔티티 → 판매자 상세 응답 DTO 변환
     */
    private SellerDealDetailResponse toDetailResponse(Deal deal) {
        String buyerNickname = "구매자";
        if (deal.getBuyer() != null && deal.getBuyer().getUserProfile() != null) {
            buyerNickname = deal.getBuyer().getUserProfile().getNickname();
        }

        return new SellerDealDetailResponse(
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
                deal.getBuyer() != null ? deal.getBuyer().getUserId() : null,
                buyerNickname
        );
    }
}
