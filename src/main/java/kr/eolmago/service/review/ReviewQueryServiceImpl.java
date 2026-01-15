package kr.eolmago.service.review;

import kr.eolmago.dto.api.review.response.ReceivedReviewDto;
import kr.eolmago.dto.api.review.response.ReceivedReviewListResponse;
import kr.eolmago.repository.review.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewQueryServiceImpl implements ReviewQueryService {

    private final ReviewRepository reviewRepository;

    @Override
    public ReceivedReviewListResponse getReceivedReviews(UUID sellerId, int page, int size) {

        Pageable pageable = PageRequest.of(page, size);

        Page<ReceivedReviewDto> result =
                reviewRepository.findReceivedReviewsBySellerId(sellerId, pageable);

        Double avg = reviewRepository.findAverageRatingBySeller(sellerId);
        long totalCount = reviewRepository.countBySeller_UserId(sellerId);

        List<ReceivedReviewDto> items = result.getContent();

        return new ReceivedReviewListResponse(
                sellerId,
                avg == null ? 0.0 : avg,
                totalCount,
                result.getNumber(),
                result.getSize(),
                result.getTotalPages(),
                result.getTotalElements(),
                items
        );
    }
}
