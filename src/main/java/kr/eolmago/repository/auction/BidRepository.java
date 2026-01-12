package kr.eolmago.repository.auction;

import kr.eolmago.domain.entity.auction.Bid;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BidRepository extends JpaRepository<Bid, Long>, BidRepositoryCustom {

}
