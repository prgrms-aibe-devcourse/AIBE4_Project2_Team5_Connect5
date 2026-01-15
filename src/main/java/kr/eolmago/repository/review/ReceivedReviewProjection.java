package kr.eolmago.repository.review;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface ReceivedReviewProjection {

    Long getReviewId();

    Long getDealId();

    Integer getRating();

    String getContent();

    OffsetDateTime getCreatedAt();

    UUID getBuyerId();
}

