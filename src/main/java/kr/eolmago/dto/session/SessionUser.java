package kr.eolmago.dto.session;

import kr.eolmago.domain.entity.user.User;
import lombok.Getter;

import java.io.Serializable;
import java.util.UUID;

/**
 * 세션에 저장할 사용자 정보 DTO (직렬화 가능)
 */
@Getter
public class SessionUser implements Serializable {

    private static final long serialVersionUID = 1L;

    private final UUID userId;
    private final String role;
    private final String name;

    public SessionUser(User user) {
        this.userId = user.getUserId();
        this.role = user.getRole().name();
        this.name = "User"; // 나중에 UserProfile에서 name 가져오기
    }

    public static SessionUser from(User user) {
        return new SessionUser(user);
    }
}
