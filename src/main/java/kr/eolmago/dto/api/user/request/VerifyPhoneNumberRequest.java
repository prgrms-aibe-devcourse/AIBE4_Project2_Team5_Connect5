package kr.eolmago.dto.api.user.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyPhoneNumberRequest(
        @NotBlank(message = "핸드폰 번호는 필수입니다")
        @Pattern(regexp = "^\\d{10,11}$", message = "핸드폰번호는 10~11자리의 숫자여야 합니다")
        String phoneNumber,

        @NotBlank(message = "인증 코드는 필수입니다")
        @Pattern(regexp = "^\\d{6}$", message = "인증 코드는 6자리의 숫자여야 합니다")
        String verificationCode
) {

}
