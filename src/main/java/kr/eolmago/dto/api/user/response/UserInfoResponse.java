package kr.eolmago.dto.api.user.response;

import java.util.UUID;

public record UserInfoResponse(
        UUID id,
        String email,
        String nickname,
        String role
) {
}
