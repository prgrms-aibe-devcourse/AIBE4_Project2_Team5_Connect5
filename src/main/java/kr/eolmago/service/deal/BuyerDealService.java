package kr.eolmago.service.deal;

import kr.eolmago.domain.entity.deal.Deal;
import kr.eolmago.dto.api.deal.response.BuyerDealListResponse;
import kr.eolmago.global.exception.BusinessException;
import kr.eolmago.global.exception.ErrorCode;
import kr.eolmago.repository.deal.DealRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 구매자 거래 조회 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BuyerDealService {

    private final DealRepository dealRepository;

    /**
     * 구매자의 모든 거래 조회
     */
    public BuyerDealListResponse getBuyerDeals(UUID buyerId) {
        List<Deal> deals = dealRepository.findByBuyer_UserId(buyerId);
        
        List<BuyerDealListResponse.DealDto> dealDtos = deals.stream()
                .map(deal -> new BuyerDealListResponse.DealDto(
                        deal.getDealId(),
                        deal.getFinalPrice(),
                        deal.getStatus().name(),
                        deal.getCreatedAt() != null ? deal.getCreatedAt().toString() : null,
                        deal.getBuyerConfirmedAt() != null ? deal.getBuyerConfirmedAt().toString() : null // 값 추가
                ))
                .toList();
        
        return new BuyerDealListResponse(
                buyerId,
                deals.size(),
                dealDtos
        );
    }

    /**
     * 구매자의 특정 거래 상세 조회 (권한 검증)
     */
    public BuyerDealListResponse.DealDto getDealDetail(Long dealId, UUID buyerId) {
        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEAL_NOT_FOUND));
        
        // 권한 검증: 내가 구매자인 거래인지 확인
        if (!deal.getBuyer().getUserId().equals(buyerId)) {
            throw new BusinessException(ErrorCode.DEAL_UNAUTHORIZED);
        }
        
        return new BuyerDealListResponse.DealDto(
                deal.getDealId(),
                deal.getFinalPrice(),
                deal.getStatus().name(),
                deal.getCreatedAt() != null ? deal.getCreatedAt().toString() : null,
                deal.getBuyerConfirmedAt() != null ? deal.getBuyerConfirmedAt().toString() : null // 값 추가
        );
    }

    /**
     * 구매자 거래 확정
     */
    @Transactional
    public void confirmDeal(Long dealId, UUID buyerId) {
        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEAL_NOT_FOUND));

        // 권한 검증: 내가 구매자인 거래인지 확인
        if (!deal.getBuyer().getUserId().equals(buyerId)) {
            throw new BusinessException(ErrorCode.DEAL_UNAUTHORIZED);
        }

        // 이미 확정된 경우 예외 처리
        if (deal.getBuyerConfirmedAt() != null) {
            throw new IllegalStateException("이미 구매자 확인이 완료된 거래입니다.");
        }

        deal.confirmByBuyer();
    }
}
