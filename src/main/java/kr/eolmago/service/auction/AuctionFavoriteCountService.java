package kr.eolmago.service.auction;

import kr.eolmago.repository.auction.AuctionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuctionFavoriteCountService {

    private final AuctionRepository auctionRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void incrementFavoriteCount(UUID auctionId) {
        auctionRepository.incrementFavoriteCount(auctionId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void decrementFavoriteCount(UUID auctionId) {
        auctionRepository.decrementFavoriteCount(auctionId);
    }
}