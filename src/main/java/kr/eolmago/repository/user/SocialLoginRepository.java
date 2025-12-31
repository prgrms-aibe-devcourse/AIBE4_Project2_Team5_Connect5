package kr.eolmago.repository.user;

import kr.eolmago.domain.entity.user.SocialLogin;
import kr.eolmago.domain.entity.user.User;
import kr.eolmago.domain.entity.user.enums.SocialProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SocialLoginRepository extends JpaRepository<SocialLogin, Long> {

    Optional<SocialLogin> findByProviderAndProviderId(SocialProvider provider, String providerId);

    List<SocialLogin> findByUser(User user);

    Optional<SocialLogin> findByUserAndProvider(User user, String provider);

    boolean existsByProviderAndProviderId(SocialProvider provider, String providerId);

    boolean existsByUserAndProvider(User user, SocialProvider provider);

    List<SocialLogin> findByEmail(String email);

    Optional<SocialLogin> findByUserAndEmail(User user, String email);

    List<SocialLogin> findByProvider(SocialProvider provider);
}
