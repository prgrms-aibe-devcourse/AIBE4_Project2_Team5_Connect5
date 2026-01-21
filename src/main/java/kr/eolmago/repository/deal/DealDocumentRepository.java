package kr.eolmago.repository.deal;

import kr.eolmago.domain.entity.deal.Deal;
import kr.eolmago.domain.entity.deal.DealDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DealDocumentRepository extends JpaRepository<DealDocument, Long> {

    /**
     * Deal로 문서 존재 여부 확인
     */
    boolean existsByDeal(Deal deal);


}
