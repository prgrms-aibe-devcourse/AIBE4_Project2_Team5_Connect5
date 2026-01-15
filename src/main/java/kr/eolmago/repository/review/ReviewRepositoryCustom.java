package kr.eolmago.repository.review;

import java.util.List;
import java.util.UUID;

public interface ReviewRepositoryCustom {

    List<Long> findReviewedDealIds(List<Long> dealIds);

    Double findAverageRatingBySeller(UUID sellerId);

}