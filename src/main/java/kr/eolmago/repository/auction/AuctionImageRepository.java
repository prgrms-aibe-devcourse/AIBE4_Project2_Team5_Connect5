package kr.eolmago.repository.auction;

import kr.eolmago.domain.entity.auction.AuctionImage;
import kr.eolmago.domain.entity.auction.AuctionItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuctionImageRepository extends JpaRepository<AuctionImage, Long> {

    List<AuctionImage> findByAuctionItemOrderByDisplayOrder(AuctionItem auctionItem);

    void deleteByAuctionItem(AuctionItem auctionItem);
}
