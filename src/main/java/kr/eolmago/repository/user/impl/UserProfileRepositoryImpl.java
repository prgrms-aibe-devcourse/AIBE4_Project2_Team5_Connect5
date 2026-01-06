package kr.eolmago.repository.user.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import kr.eolmago.domain.entity.user.UserProfile;
import kr.eolmago.repository.user.UserProfileRepositoryCustom;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.UUID;

import static kr.eolmago.domain.entity.user.QUser.user;
import static kr.eolmago.domain.entity.user.QUserProfile.userProfile;

@Slf4j
public class UserProfileRepositoryImpl implements UserProfileRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public UserProfileRepositoryImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public Optional<UserProfile> findByUserIdWithUser(UUID userId) {
        log.debug("프로필 조회: userId={}", userId);

        UserProfile result = queryFactory
                .selectFrom(userProfile)
                .leftJoin(userProfile.user, user).fetchJoin()
                .where(userProfile.user.userId.eq(userId))
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public boolean existsByNickname(String nickname) {
        log.debug("닉네임 존재 여부 확인: nickname={}", nickname);

        return queryFactory
                .selectOne()
                .from(userProfile)
                .where(userProfile.nickname.eq(nickname))
                .fetchOne() != null;
    }
}
