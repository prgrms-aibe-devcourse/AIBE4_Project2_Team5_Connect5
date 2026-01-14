package kr.eolmago.dto.api.user.response;

import java.time.OffsetDateTime;

public record UserPenaltyInfoResponse(
        String type,
        String reason,
        OffsetDateTime expiresAt
) {
    public static UserPenaltyInfoResponse of(String type, String reason, OffsetDateTime expiresAt) {
        return new UserPenaltyInfoResponse(type, reason, expiresAt);
    }
}
