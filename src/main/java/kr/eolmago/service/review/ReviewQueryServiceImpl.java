package kr.eolmago.service.review;

import kr.eolmago.dto.api.review.response.ReceivedReviewListResponse;
import kr.eolmago.repository.review.ReceivedReviewProjection;
import kr.eolmago.repository.review.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewQueryServiceImpl implements ReviewQueryService {

    private final ReviewRepository reviewRepository;

    @Override
    public ReceivedReviewListResponse getReceivedReviews(UUID sellerId, int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        // ✅ Projection 기반 조회 (fetch join + pageable 문제 회피)
        Page<ReceivedReviewProjection> result =
                reviewRepository.findReceivedReviewsBySellerId(sellerId, pageable);

        Double avg = reviewRepository.findAverageRatingBySeller(sellerId);
        long totalCount = reviewRepository.countBySeller_UserId(sellerId);

        // --------- DTO 매핑 ----------
        // ⚠️ 여기 "ReceivedReviewListResponse" 생성 방식은 너희 DTO에 맞춰 조정하면 됨
        List<ReceivedReviewListResponse.ReceivedReviewDto> items = result.getContent().stream()
                .map(p -> ReceivedReviewListResponse.ReceivedReviewDto.builder()
                        .reviewId(p.getReviewId())
                        .dealId(p.getDealId())
                        .rating(p.getRating())
                        .content(p.getContent())
                        .createdAt(p.getCreatedAt())
                        .buyerId(p.getBuyerId())
                        .buyerNickname(p.getBuyerNickname())
                        .buyerProfileImageUrl(p.getBuyerProfileImageUrl())
                        .build()
                )
                .toList();

        return ReceivedReviewListResponse.builder()
                .sellerId(sellerId)
                .averageRating(avg == null ? 0.0 : avg)
                .totalCount(totalCount)
                .page(result.getNumber())
                .size(result.getSize())
                .totalPages(result.getTotalPages())
                .totalElements(result.getTotalElements())
                .reviews(items)
                .build();
    }
}
