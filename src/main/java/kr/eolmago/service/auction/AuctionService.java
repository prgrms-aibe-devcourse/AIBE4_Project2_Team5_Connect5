package kr.eolmago.service.auction;

import kr.eolmago.domain.entity.auction.Auction;
import kr.eolmago.domain.entity.auction.AuctionImage;
import kr.eolmago.domain.entity.auction.AuctionItem;
import kr.eolmago.domain.entity.auction.Bid;
import kr.eolmago.domain.entity.auction.enums.AuctionStatus;
import kr.eolmago.domain.entity.auction.enums.ItemCategory;
import kr.eolmago.domain.entity.auction.enums.ItemCondition;
import kr.eolmago.domain.entity.user.User;
import kr.eolmago.dto.api.auction.request.AuctionDraftRequest;
import kr.eolmago.dto.api.auction.request.AuctionSearchRequest;
import kr.eolmago.dto.api.auction.response.*;
import kr.eolmago.dto.api.common.PageResponse;
import kr.eolmago.global.exception.BusinessException;
import kr.eolmago.global.exception.ErrorCode;
import kr.eolmago.global.util.BidIncrementCalculator;
import kr.eolmago.repository.auction.AuctionImageRepository;
import kr.eolmago.repository.auction.AuctionItemRepository;
import kr.eolmago.repository.auction.AuctionRepository;
import kr.eolmago.repository.auction.BidRepository;
import kr.eolmago.repository.user.UserRepository;
import kr.eolmago.service.auction.event.AuctionEndAtChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuctionService {

    private final AuctionRepository auctionRepository;
    private final AuctionItemRepository auctionItemRepository;
    private final AuctionImageRepository auctionImageRepository;
    private final BidRepository bidRepository;
    private final UserRepository userRepository;

    private final AuctionCloseService auctionCloseService;
    private final ApplicationEventPublisher eventPublisher;

    // 임시저장 생성
    @Transactional
    public AuctionDraftResponse createDraft(AuctionDraftRequest request, UUID sellerId) {

        User sellerRef = userRepository.getReferenceById(sellerId);

        // 1. AuctionItem 데이터 저장
        AuctionItem auctionItem = AuctionItem.create(
                request.itemName(),
                request.category(),
                request.condition(),
                request.specs()
        );
        auctionItemRepository.save(auctionItem);

        // 2. AuctionImage 데이터 저장
        List<AuctionImage> images = new ArrayList<>();
        for (int i = 0; i < request.imageUrls().size(); i++) {
            images.add(AuctionImage.create(
                    auctionItem,
                    request.imageUrls().get(i),
                    i // displayOrder
            ));
        }
        auctionImageRepository.saveAll(images);

        // 시작가 기준으로 입찰 단위를 자동 계산
        int bidIncrement = BidIncrementCalculator.calculate(request.startPrice());

        // 3. Auction 데이터 저장
        Auction auction = Auction.create(
                auctionItem,
                sellerRef,
                request.title(),
                request.description(),
                AuctionStatus.DRAFT,
                request.startPrice(),
                bidIncrement,
                request.durationHours(),
                null,
                null
        );
        auctionRepository.save(auction);

        return new AuctionDraftResponse(sellerId, auction.getAuctionId(), auction.getStatus());
    }

    // 임시저장 수정
    // DRAFT일 때만 수정 가능
    @Transactional
    public AuctionDraftResponse updateDraft(UUID auctionId, AuctionDraftRequest request, UUID sellerId) {

        Auction auction = loadDraftOwnedAuction(auctionId, sellerId, ErrorCode.AUCTION_NOT_DRAFT);

        // bidIncrement 재계산
        int bidIncrement = BidIncrementCalculator.calculate(request.startPrice());

        // Auction 필드 업데이트
        auction.updateDraft(
                request.title(),
                request.description(),
                request.startPrice(),
                bidIncrement,
                request.durationHours()
        );

        // AuctionItem 업데이트
        AuctionItem item = auction.getAuctionItem();
        item.updateDraft(
                request.itemName(),
                request.category(),
                request.condition(),
                request.specs()
        );

        // 이미지 교체 - 삭제 후 재삽입
        auctionImageRepository.deleteByAuctionItem(item); // 삭제
        auctionImageRepository.flush(); // flush 타이밍 이슈 방지

        List<AuctionImage> newImages = new ArrayList<>();
        for (int i = 0; i < request.imageUrls().size(); i++) {
            newImages.add(AuctionImage.create(item, request.imageUrls().get(i), i));
        }
        auctionImageRepository.saveAll(newImages);

        return new AuctionDraftResponse(sellerId, auction.getAuctionId(), auction.getStatus());
    }

    // 경매 임시저장 초기화
    @Transactional
    public AuctionDraftResponse initDraft(UUID sellerId) {

        final String initTitle = "작성 중인 경매";
        final String initItemName = "작성 중인 상품";
        final int initStartPrice = 10_000;
        final int initDurationHours = 12;

        User sellerRef = userRepository.getReferenceById(sellerId);

        ItemCategory defaultCategory = ItemCategory.values()[0];
        ItemCondition defaultCondition = ItemCondition.values()[0];

        AuctionItem auctionItem = AuctionItem.create(
                initItemName,
                defaultCategory,
                defaultCondition,
                new HashMap<>()
        );
        auctionItemRepository.save(auctionItem);

        int bidIncrement = BidIncrementCalculator.calculate(initStartPrice);

        Auction auction = Auction.create(
                auctionItem,
                sellerRef,
                initTitle,
                null,
                AuctionStatus.DRAFT,
                initStartPrice,
                bidIncrement,
                initDurationHours,
                null,
                null
        );
        auctionRepository.save(auction);

        return new AuctionDraftResponse(sellerId, auction.getAuctionId(), auction.getStatus());
    }

    // 경매 삭제
    @Transactional
    public void deleteAuction(UUID auctionId, UUID sellerId) {

        Auction auction = loadDraftOwnedAuction(auctionId, sellerId, ErrorCode.AUCTION_DELETE_ONLY_DRAFT);

        AuctionItem item = auction.getAuctionItem();
        auctionImageRepository.deleteByAuctionItem(item);
        auctionRepository.delete(auction);
        auctionItemRepository.delete(item);
    }

    // 경매 게시
    @Transactional
    public AuctionDraftResponse publishAuction(UUID auctionId, UUID sellerId) {

        Auction auction = loadDraftOwnedAuction(auctionId, sellerId, ErrorCode.AUCTION_PUBLISH_ONLY_DRAFT);

        Integer durationHours = auction.getDurationHours();
        if (durationHours == null) {
            throw new BusinessException(ErrorCode.AUCTION_INVALID_DURATION);
        }

        // startAt, endAt, originalEndAt 갱신
        OffsetDateTime startAt = OffsetDateTime.now();
        OffsetDateTime endAt = startAt.plusHours(durationHours);
        auction.publish(startAt, endAt);

        eventPublisher.publishEvent(new AuctionEndAtChangedEvent(auction.getAuctionId(), endAt));

        return new AuctionDraftResponse(sellerId, auction.getAuctionId(), auction.getStatus());
    }

    // 경매 목록 조회
    public PageResponse<AuctionListResponse> getAuction(
            int page,
            int size,
            String sortKey,
            AuctionSearchRequest searchRequest
    ) {
        Pageable pageable = PageRequest.of(page, size);

        Page<AuctionListDto> dtoPage = auctionRepository.searchList(pageable, sortKey, searchRequest);
        Page<AuctionListResponse> responsePage = dtoPage.map(AuctionListResponse::from);

        return PageResponse.of(responsePage);
    }

    // 경매 임시저장 단건 조회
    public AuctionDraftDetailResponse getDraft(UUID auctionId, UUID sellerId) {

        Auction auction = loadDraftOwnedAuction(auctionId, sellerId, ErrorCode.AUCTION_NOT_DRAFT);

        AuctionItem item = auction.getAuctionItem();

        List<String> imageUrls = auctionImageRepository.findByAuctionItemOrderByDisplayOrder(item)
                .stream()
                .map(AuctionImage::getImageUrl)
                .toList();

        return new AuctionDraftDetailResponse(
                auction.getAuctionId(),
                sellerId,
                auction.getTitle(),
                auction.getDescription(),
                auction.getStartPrice(),
                auction.getDurationHours(),
                auction.getBidIncrement(),
                item.getItemName(),
                item.getCategory(),
                item.getCondition(),
                item.getSpecs(),
                imageUrls
        );
    }

    // 경매 상세 조회
    public AuctionDetailResponse getAuctionDetail(UUID auctionId) {

        AuctionDetailDto dto = auctionRepository.findDetailById(auctionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUCTION_NOT_FOUND));

        // 모든 이미지 URL 조회
        AuctionItem itemRef = auctionItemRepository.getReferenceById(dto.auctionItemId());
        List<String> imageUrls = auctionImageRepository.findByAuctionItemOrderByDisplayOrder(itemRef)
                .stream()
                .map(AuctionImage::getImageUrl)
                .toList();

        // 최고 입찰자 ID 조회
        Auction auctionRef = auctionRepository.getReferenceById(auctionId);
        UUID highestBidderId = bidRepository.findTopBidderIdByAuction(auctionRef).orElse(null);

        AuctionDetailDto dtoWithHighestBidder = dto.withHighestBidderId(highestBidderId);

        return AuctionDetailResponse.from(dtoWithHighestBidder, imageUrls);
    }

    public void closeAuction(UUID auctionId) {
        auctionCloseService.closeAuction(auctionId);
    }

    // 경매 조회, 소유자 검증, DRAFT 상태 검증
    private Auction loadDraftOwnedAuction(UUID auctionId, UUID sellerId, ErrorCode notDraftError) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUCTION_NOT_FOUND));

        if (!auction.getSeller().getUserId().equals(sellerId)) {
            throw new BusinessException(ErrorCode.AUCTION_UNAUTHORIZED);
        }

        if (auction.getStatus() != AuctionStatus.DRAFT) {
            throw new BusinessException(notDraftError);
        }

        return auction;
    }
}