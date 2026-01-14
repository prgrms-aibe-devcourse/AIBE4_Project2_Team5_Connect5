package kr.eolmago.dto.api.admin.response;

import kr.eolmago.domain.entity.user.enums.UserRole;
import kr.eolmago.domain.entity.user.enums.UserStatus;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
public record UserAdminResponse(
        UUID userId,
        String nickname,
        String email,
        String phone,
        UserStatus status,
        UserRole role,
        OffsetDateTime createdAt,
        String profileImageUrl
) {}
