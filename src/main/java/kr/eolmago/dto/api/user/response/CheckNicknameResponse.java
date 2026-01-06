package kr.eolmago.dto.api.user.response;

public record CheckNicknameResponse(
        String nickname,
        boolean available,
        String message
) {
    public static CheckNicknameResponse available(String nickname) {
        return new CheckNicknameResponse(nickname, true, "사용 가능한 닉네임입니다");
    }

    public static CheckNicknameResponse duplicate(String nickname) {
        return new CheckNicknameResponse(nickname, false, "이미 사용 중인 닉네임입니다");
    }
}
