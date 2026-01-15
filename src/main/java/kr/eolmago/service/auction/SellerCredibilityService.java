package kr.eolmago.service.auction;

import kr.eolmago.domain.entity.deal.enums.DealStatus;
import kr.eolmago.dto.api.auction.response.SellerCredibilityResponse;
import kr.eolmago.global.exception.BusinessException;
import kr.eolmago.global.exception.ErrorCode;
import kr.eolmago.repository.auction.AuctionRepository;
import kr.eolmago.repository.deal.DealRepository;
import kr.eolmago.repository.report.ReportRepository;
import kr.eolmago.repository.user.UserPenaltyRepository;
import kr.eolmago.repository.user.UserProfileRepository;
import kr.eolmago.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerCredibilityService {

    private final AuctionRepository auctionRepository;

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    private final DealRepository dealRepository;
    private final ReportRepository reportRepository;

    public SellerCredibilityResponse getSellerCredibility(UUID auctionId) {
        UUID sellerId = auctionRepository.findSellerIdByAuctionId(auctionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUCTION_NOT_FOUND));

        String email = userRepository.findEmailById(sellerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        String sellerAccount = extractLocalPart(email);

        String nickname = userProfileRepository.findNicknameByUserId(sellerId).orElse(null);

        long completedDealCount = dealRepository.countBySellerIdAndStatus(sellerId, DealStatus.COMPLETED);
        long reportCount = reportRepository.countByReportedUserId(sellerId);

        return new SellerCredibilityResponse(
                sellerAccount,
                nickname,
                completedDealCount,
                reportCount
        );
    }

    private String extractLocalPart(String email) {
        if (email == null || email.isBlank()) return null;
        int at = email.indexOf('@');
        if (at <= 0) return email; // 혹시 @가 없으면 전체를 반환
        return email.substring(0, at);
    }
}