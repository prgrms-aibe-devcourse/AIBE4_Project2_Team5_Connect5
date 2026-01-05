package kr.eolmago.repository.user.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.eolmago.domain.entity.user.SocialLogin;
import kr.eolmago.domain.entity.user.enums.SocialProvider;
import kr.eolmago.repository.user.SocialLoginRepositoryCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static kr.eolmago.domain.entity.user.QSocialLogin.socialLogin;
import static kr.eolmago.domain.entity.user.QUser.user;

@Repository
@RequiredArgsConstructor
public class SocialLoginRepositoryImpl implements SocialLoginRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<SocialLogin> findByProviderAndProviderId(SocialProvider provider, String providerId) {
        SocialLogin result = queryFactory
                .selectFrom(socialLogin)
                .leftJoin(socialLogin.user, user).fetchJoin()
                .where(
                        socialLogin.provider.eq(provider),
                        socialLogin.providerId.eq(providerId)
                )
                .fetchOne();

        return Optional.ofNullable(result);
    }
}
