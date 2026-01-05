package kr.eolmago.repository.user;

import kr.eolmago.domain.entity.user.SocialLogin;
import kr.eolmago.domain.entity.user.enums.SocialProvider;

import java.util.Optional;

public interface SocialLoginRepositoryCustom {

    Optional<SocialLogin> findByProviderAndProviderId(SocialProvider provider, String providerId);
}
