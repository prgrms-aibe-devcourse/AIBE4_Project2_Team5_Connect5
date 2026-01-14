package kr.eolmago.dto.api.admin.response;

import kr.eolmago.domain.entity.report.enums.ReportReason;
import kr.eolmago.domain.entity.report.enums.ReportStatus;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
public record ReportAdminResponse(
        Long reportId,
        UUID reportedUserId,
        String reportedUserNickname,
        UUID reporterUserId,
        String reporterNickname,
        UUID auctionId, // Long -> UUID로 수정
        ReportReason reason,
        String description,
        ReportStatus status,
        OffsetDateTime createdAt
) {
}
