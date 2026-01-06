package kr.eolmago.repository.user;

import kr.eolmago.domain.entity.user.User;
import kr.eolmago.domain.entity.user.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID>, UserProfileRepositoryCustom {

    Optional<UserProfile> findByUser(User user);

    Optional<UserProfile> findByNickname(String nickname);

    // userId로 프로필 존재 여부 확인
    boolean existsByUser_UserId(UUID userId);
}
