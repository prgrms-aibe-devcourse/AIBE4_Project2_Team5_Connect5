package kr.eolmago.dto.api.admin.response;

import kr.eolmago.domain.entity.report.enums.ReportAction;
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
        String reportedUserProfileImage,
        UUID reporterUserId,
        String reporterNickname,
        String reporterProfileImage,
        UUID auctionId, // Long -> UUID로 수정
        ReportReason reason,
        String description,
        ReportStatus status,
        ReportAction action,
        OffsetDateTime createdAt
) {
}
