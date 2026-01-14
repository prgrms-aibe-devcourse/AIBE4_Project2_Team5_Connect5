package kr.eolmago.dto.api.admin.response;

import kr.eolmago.domain.entity.user.enums.PenaltyType;
import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
public record PenaltyHistoryResponse(
        Long penaltyId,
        PenaltyType type,
        String reason,
        OffsetDateTime startedAt,
        OffsetDateTime expiresAt,
        boolean isActive
) {}
