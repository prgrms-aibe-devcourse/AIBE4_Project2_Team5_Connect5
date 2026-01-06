package kr.eolmago.repository.user;

import kr.eolmago.domain.entity.user.UserProfile;

import java.util.Optional;
import java.util.UUID;

public interface UserProfileRepositoryCustom {

    // userId로 프로필 조회 (페치 조인)
    // 성능: N+1 문제 해결
    Optional<UserProfile> findByUserIdWithUser(UUID userId);

    // nickname 중복 여부 확인
    boolean existsByNickname(String nickname);
}
