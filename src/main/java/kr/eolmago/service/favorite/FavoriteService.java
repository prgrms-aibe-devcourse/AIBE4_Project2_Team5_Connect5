package kr.eolmago.service.favorite;

import kr.eolmago.domain.entity.auction.Auction;
import kr.eolmago.domain.entity.auction.Favorite;
import kr.eolmago.domain.entity.user.User;
import kr.eolmago.dto.api.common.PageResponse;
import kr.eolmago.dto.api.favorite.request.FavoriteStatusRequest;
import kr.eolmago.dto.api.favorite.response.FavoriteAuctionDto;
import kr.eolmago.dto.api.favorite.response.FavoriteAuctionResponse;
import kr.eolmago.dto.api.favorite.response.FavoriteStatusResponse;
import kr.eolmago.dto.api.favorite.response.FavoriteToggleResponse;
import kr.eolmago.global.exception.BusinessException;
import kr.eolmago.global.exception.ErrorCode;
import kr.eolmago.repository.auction.AuctionRepository;
import kr.eolmago.repository.favorite.FavoriteRepository;
import kr.eolmago.repository.user.UserRepository;
import kr.eolmago.service.auction.AuctionFavoriteCountService;
import kr.eolmago.service.notification.publish.NotificationPublishCommand;
import kr.eolmago.service.notification.publish.NotificationPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;
    private final NotificationPublisher notificationPublisher;
    private final AuctionFavoriteCountService auctionFavoriteCountService;

    @Transactional
    public FavoriteToggleResponse toggleFavorite(UUID userId, UUID auctionId) {

        // 사용자 검증
        // - userId는 인증된 사용자 기준이어야 한다.
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 경매 존재 검증
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUCTION_NOT_FOUND));

        // 본인 경매 찜 금지
        if (auction.getSeller() != null && userId.equals(auction.getSeller().getUserId())) {
            throw new BusinessException(ErrorCode.FAVORITE_SELF_AUCTION_FORBIDDEN);
        }

        // 토글 처리
        boolean favorited =  favoriteRepository.findByUserAndAuction(userId, auctionId)
                .map(existing -> {
                    // 찜 삭제
                    favoriteRepository.delete(existing);
                    auctionFavoriteCountService.decrementFavoriteCount(auctionId);
                    return false;
                })
                .orElseGet(() -> {
                    try {
                        // 찜 추가
                        Favorite favorite = Favorite.create(user, auction);
                        favoriteRepository.save(favorite);
                        auctionFavoriteCountService.incrementFavoriteCount(auctionId);
                        notificationPublisher.publish(
                            NotificationPublishCommand.favoriteAdded(userId, auctionId)
                        );
                        return true;
                    } catch (DataIntegrityViolationException e) {
                        // 중복 찜 방어
                        // - 더블클릭/재시도/동시 요청으로 unique 충돌이 날 수 있다.
                        throw new BusinessException(ErrorCode.FAVORITE_ALREADY_EXISTS);
                    }
                });

        // 최신 카운트 조회 후 DTO 응답
        int favoriteCount = auctionRepository.findFavoriteCountByAuctionId(auctionId);
        return new FavoriteToggleResponse(auctionId, favorited, favoriteCount);
    }

    public FavoriteStatusResponse getFavoriteStatuses(UUID userId, FavoriteStatusRequest request) {
        List<UUID> auctionIds = Optional.ofNullable(request.auctionIds()).orElse(List.of());
        if (auctionIds.isEmpty()) {
            return new FavoriteStatusResponse(Collections.emptyMap());
        }

        List<UUID> favoritedIds = favoriteRepository.findFavoritedAuctionIds(userId, auctionIds);
        Set<UUID> favoritedSet = new HashSet<>(favoritedIds);

        Map<UUID, Boolean> result = auctionIds.stream()
                .collect(Collectors.toMap(
                        id -> id,
                        favoritedSet::contains,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        return new FavoriteStatusResponse(result);
    }

    // 내 찜 목록 조회
    public PageResponse<FavoriteAuctionResponse> getMyFavorites(UUID userId, Pageable pageable, String filter, String sort) {

        Page<FavoriteAuctionDto> page = favoriteRepository.searchMyFavorites(pageable, userId, filter, sort);

        Page<FavoriteAuctionResponse> mapped = page.map(FavoriteAuctionResponse::from);

        return PageResponse.of(mapped);
    }
}
