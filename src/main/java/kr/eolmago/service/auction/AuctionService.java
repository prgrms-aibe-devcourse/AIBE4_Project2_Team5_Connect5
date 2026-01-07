package kr.eolmago.service.auction;

import kr.eolmago.domain.entity.auction.Auction;
import kr.eolmago.domain.entity.auction.AuctionItem;
import kr.eolmago.domain.entity.auction.enums.AuctionStatus;
import kr.eolmago.domain.entity.user.User;
import kr.eolmago.dto.api.auction.request.AuctionCreateRequest;
import kr.eolmago.dto.api.auction.request.AuctionUpdateRequest;
import kr.eolmago.dto.api.auction.response.AuctionListResponse;
import kr.eolmago.dto.api.common.PageResponse;
import kr.eolmago.dto.api.auction.response.AuctionListDto;
import kr.eolmago.repository.auction.AuctionItemRepository;
import kr.eolmago.repository.auction.AuctionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuctionService {

    private final AuctionRepository auctionRepository;
    private final AuctionItemRepository auctionItemRepository;

    /**
     * 경매 생성 (DRAFT 또는 LIVE 상태)
     */
    @Transactional
    public Auction createAuction(AuctionCreateRequest request, User seller) {
        // 1. AuctionItem 생성
        AuctionItem auctionItem = AuctionItem.create(
                request.itemName(),
                request.category(),
                request.condition(),
                request.specs()
        );
        auctionItemRepository.save(auctionItem);

        // 2. 상태에 따라 시작/종료 시간 설정
        OffsetDateTime startAt = null;
        OffsetDateTime endAt = null;

        if (request.status() == AuctionStatus.LIVE) {
            startAt = OffsetDateTime.now();
            endAt = startAt.plusHours(request.durationHours());
        }

        // 3. Auction 생성
        Auction auction = Auction.create(
                auctionItem,
                seller,
                request.title(),
                request.description(),
                request.status(),
                request.startPrice(),
                request.durationHours(),
                startAt,
                endAt
        );
        auctionRepository.save(auction);

        return auction;
    }

    /**
     * 경매 수정 (DRAFT 상태에서만 가능)
     */
    @Transactional
    public void updateAuction(UUID auctionId, AuctionUpdateRequest request, User seller) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("경매를 찾을 수 없습니다."));

        // 권한 확인
        if (!auction.getSeller().getUserId().equals(seller.getUserId())) {
            throw new IllegalArgumentException("경매를 수정할 권한이 없습니다.");
        }

        // DRAFT 상태 확인 및 수정
        auction.updateDraft(
                request.title(),
                request.description(),
                request.startPrice()
        );
    }

    /**
     * 경매 삭제 (DRAFT 상태에서만 가능)
     */
    @Transactional
    public void deleteAuction(UUID auctionId, User seller) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("경매를 찾을 수 없습니다."));

        // 권한 확인
        if (!auction.getSeller().getUserId().equals(seller.getUserId())) {
            throw new IllegalArgumentException("경매를 삭제할 권한이 없습니다.");
        }

        // DRAFT 상태 확인
        if (auction.getStatus() != AuctionStatus.DRAFT) {
            throw new IllegalStateException("DRAFT 상태에서만 삭제 가능합니다.");
        }

        auctionRepository.delete(auction);
    }

    /**
     * 경매 게시 (DRAFT → LIVE)
     */
    @Transactional
    public void publishAuction(UUID auctionId, User seller) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("경매를 찾을 수 없습니다."));

        // 권한 확인
        if (!auction.getSeller().getUserId().equals(seller.getUserId())) {
            throw new IllegalArgumentException("경매를 게시할 권한이 없습니다.");
        }

        // 시작 시간과 종료 시간 계산
        OffsetDateTime startAt = OffsetDateTime.now();
        OffsetDateTime endAt = startAt.plusHours(auction.getDurationHours());

        // DRAFT 상태 확인 및 게시
        auction.publish(startAt, endAt);
    }

    /**
     * 경매 조회 (목록)
     */
    public PageResponse<AuctionListResponse> getAuctions(int page, int size, String sortKey, AuctionStatus status, UUID sellerId) {
        Pageable pageable = PageRequest.of(page, size);

        Page<AuctionListDto> dtoPage = auctionRepository.searchList(pageable, sortKey, status, sellerId);
        Page<AuctionListResponse> responsePage = dtoPage.map(AuctionListResponse::from);

        return PageResponse.of(responsePage);
    }

}