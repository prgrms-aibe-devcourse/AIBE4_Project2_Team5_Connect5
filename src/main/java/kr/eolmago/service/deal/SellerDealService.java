package kr.eolmago.service.deal;

import kr.eolmago.domain.entity.deal.Deal;
import kr.eolmago.dto.api.deal.response.SellerDealListResponse;
import kr.eolmago.global.exception.BusinessException;
import kr.eolmago.global.exception.ErrorCode;
import kr.eolmago.repository.deal.DealRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 판매자 거래 조회 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerDealService {

    private final DealRepository dealRepository;

    /**
     * 판매자의 모든 거래 조회
     */
    public SellerDealListResponse getSellerDeals(UUID sellerId) {
        List<Deal> deals = dealRepository.findBySeller_UserId(sellerId);
        
        List<SellerDealListResponse.DealDto> dealDtos = deals.stream()
                .map(deal -> new SellerDealListResponse.DealDto(
                        deal.getDealId(),
                        deal.getFinalPrice(),
                        deal.getStatus().name(),
                        deal.getCreatedAt() != null ? deal.getCreatedAt().toString() : null,
                        deal.getSellerConfirmedAt() != null ? deal.getSellerConfirmedAt().toString() : null // 값 추가
                ))
                .toList();
        
        return new SellerDealListResponse(
                sellerId,
                deals.size(),
                dealDtos
        );
    }

    /**
     * 판매자의 특정 거래 상세 조회 (권한 검증)
     */
    public SellerDealListResponse.DealDto getDealDetail(Long dealId, UUID sellerId) {
        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEAL_NOT_FOUND));
        
        // 권한 검증: 내가 판매자인 거래인지 확인
        if (!deal.getSeller().getUserId().equals(sellerId)) {
            throw new BusinessException(ErrorCode.DEAL_UNAUTHORIZED);
        }
        
        return new SellerDealListResponse.DealDto(
                deal.getDealId(),
                deal.getFinalPrice(),
                deal.getStatus().name(),
                deal.getCreatedAt() != null ? deal.getCreatedAt().toString() : null,
                deal.getSellerConfirmedAt() != null ? deal.getSellerConfirmedAt().toString() : null // 값 추가
        );
    }

    /**
     * 판매자 거래 확정
     */
    @Transactional
    public void confirmDeal(Long dealId, UUID sellerId) {
        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEAL_NOT_FOUND));

        // 권한 검증: 내가 판매자인 거래인지 확인
        if (!deal.getSeller().getUserId().equals(sellerId)) {
            throw new BusinessException(ErrorCode.DEAL_UNAUTHORIZED);
        }

        // 이미 확정된 경우 예외 처리
        if (deal.getSellerConfirmedAt() != null) {
            throw new IllegalStateException("이미 판매자 확인이 완료된 거래입니다.");
        }

        deal.confirmBySeller();
    }
}
