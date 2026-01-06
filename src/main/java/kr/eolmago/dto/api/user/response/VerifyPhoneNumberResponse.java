package kr.eolmago.dto.api.user.response;

public record VerifyPhoneNumberResponse(
        boolean verified,
        String message,
        String phoneNumber
) {
    public static VerifyPhoneNumberResponse success(String phoneNumber) {
        return new VerifyPhoneNumberResponse(true, "핸드폰 번호 인증이 완료되었습니다", phoneNumber);
    }

    public static VerifyPhoneNumberResponse failure(String phoneNumber) {
        return new VerifyPhoneNumberResponse(false, "인증 코드가 일치하지 않습니다", phoneNumber);
    }
}
