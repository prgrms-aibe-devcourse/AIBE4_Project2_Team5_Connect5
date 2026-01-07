package kr.eolmago.repository.auction;

import kr.eolmago.domain.entity.auction.Auction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuctionSearchRepository extends JpaRepository<Auction, UUID>, AuctionSearchRepositoryCustom {

}