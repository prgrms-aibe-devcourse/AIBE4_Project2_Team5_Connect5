package kr.eolmago.repository;

import kr.eolmago.domain.entity.auction.AuctionItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuctionItemRepository extends JpaRepository<AuctionItem, Long> {
}
