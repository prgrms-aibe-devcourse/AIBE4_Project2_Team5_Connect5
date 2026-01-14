package kr.eolmago.service.auction;

import kr.eolmago.domain.entity.auction.Auction;
import kr.eolmago.domain.entity.user.enums.UserRole;
import kr.eolmago.dto.api.auction.response.BidHistoryItemResponse;
import kr.eolmago.dto.api.auction.response.BidHistoryResponse;
import kr.eolmago.dto.api.auction.response.BidHistoryRow;
import kr.eolmago.dto.api.auction.response.BidderFirstBidDto;
import kr.eolmago.global.exception.BusinessException;
import kr.eolmago.global.exception.ErrorCode;
import kr.eolmago.global.security.CustomUserDetails;
import kr.eolmago.repository.auction.AuctionRepository;
import kr.eolmago.repository.auction.BidRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static kr.eolmago.service.auction.constants.AuctionConstants.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BidListService {
    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;

    public BidHistoryResponse getBidHistory(UUID auctionId, CustomUserDetails principal, int page, int size) {

        UUID requesterId = UUID.fromString(principal.getId());

        boolean isAdmin = hasRole(principal, UserRole.ADMIN.name(), "ROLE_ADMIN");

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUCTION_NOT_FOUND));

        boolean isSeller = auction.getSeller() != null
                && auction.getSeller().getUserId() != null
                && auction.getSeller().getUserId().equals(requesterId);

        // 입찰자 N으로 표시
        Map<UUID, String> bidderLabelMap = buildBidderLabelMap(auctionId);

        // 히스토리 페이지 조회
        Page<BidHistoryRow> rows = bidRepository.findBidHistory(auctionId, PageRequest.of(safePage, safeSize));

        List<BidHistoryItemResponse> items = rows.getContent().stream()
                .map(r -> {
                    UUID bidderId = r.bidderId();
                    boolean isMe = bidderId != null && bidderId.equals(requesterId);

                    boolean amountVisible;
                    if (isAdmin || isSeller) {
                        // 관리자/판매자는 입찰 금액 모두 공개
                        amountVisible = true;
                    } else {
                        // 입찰자는 본인 입찰 금액만 공개
                        amountVisible = isMe;
                    }

                    Integer amount = amountVisible ? r.amount() : null;

                    String label = bidderLabelMap.getOrDefault(bidderId, "입찰자");

                    return new BidHistoryItemResponse(
                            r.bidId(),
                            r.bidAt(),
                            amount,
                            amountVisible,
                            label,
                            isMe
                    );
                })
                .collect(Collectors.toList());

        return new BidHistoryResponse(
                auctionId,
                auction.getCurrentPrice(),
                auction.getBidCount(),
                auction.getEndAt(),
                rows.getNumber(),
                rows.getSize(),
                rows.getTotalElements(),
                rows.getTotalPages(),
                rows.isLast(),
                items
        );
    }

    // 라벨 매핑
    private Map<UUID, String> buildBidderLabelMap(UUID auctionId) {
        List<BidderFirstBidDto> bidderOrders = bidRepository.findBidderOrder(auctionId);

        Map<UUID, String> map = new HashMap<>();
        int idx = 1;
        for (BidderFirstBidDto dto : bidderOrders) {
            if (dto.bidderId() == null) continue;
            map.put(dto.bidderId(), idx + "번 입찰자");
            idx++;
        }
        return map;
    }

    private boolean hasRole(CustomUserDetails principal, String... roleCandidates) {
        if (principal.getAuthorities() == null) return false;

        for (var a : principal.getAuthorities()) {
            String auth = a.getAuthority();
            if (auth == null) continue;

            for (String c : roleCandidates) {
                if (c != null && c.equals(auth)) {
                    return true;
                }
            }
        }
        return false;
    }
}
