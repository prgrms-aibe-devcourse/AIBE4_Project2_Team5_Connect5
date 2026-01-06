package kr.eolmago.dto.api.user.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(
        @NotBlank(message = "이름은 필수입니다")
        @Size(min = 2, max = 30, message = "이름은 2자 이상 30자 이하여야 합니다")
        String name,

        @NotBlank(message = "닉네임은 필수입니다")
        @Size(min = 2, max = 30, message = "닉네임은 2자 이상 30자 이하여야 합니다")
        String nickname,

        @Pattern(regexp = "^$|^\\d{10,11}$", message = "휴대폰번호는 10~11자리의 숫자여야 합니다 (선택사항)")
        String phoneNumber,

        String profileImageUrl
) {}
