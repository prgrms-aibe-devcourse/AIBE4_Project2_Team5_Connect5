package kr.eolmago.repository.user;

import kr.eolmago.domain.entity.user.SocialLogin;
import kr.eolmago.domain.entity.user.User;
import kr.eolmago.domain.entity.user.enums.SocialProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * SocialLoginRepository
 * - JpaRepository: 기본 CRUD
 * - SocialLoginRepositoryCustom: 복잡한 QueryDSL 조회
 */
public interface SocialLoginRepository extends JpaRepository<SocialLogin, Long>, SocialLoginRepositoryCustom {

    /**
     * User로 SocialLogin 목록 조회
     */
    List<SocialLogin> findByUser(User user);

    /**
     * User와 provider로 SocialLogin 조회
     */
    Optional<SocialLogin> findByUserAndProvider(User user, SocialProvider provider);

    /**
     * provider와 providerId 존재 여부 확인
     */
    boolean existsByProviderAndProviderId(SocialProvider provider, String providerId);

    /**
     * User와 provider 존재 여부 확인
     */
    boolean existsByUserAndProvider(User user, SocialProvider provider);

    /**
     * email로 SocialLogin 목록 조회
     */
    List<SocialLogin> findByEmail(String email);

    /**
     * User와 email로 SocialLogin 조회
     */
    Optional<SocialLogin> findByUserAndEmail(User user, String email);

    /**
     * provider로 SocialLogin 목록 조회
     */
    List<SocialLogin> findByProvider(SocialProvider provider);
}
