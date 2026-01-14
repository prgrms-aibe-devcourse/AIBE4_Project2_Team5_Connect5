package kr.eolmago.repository.user.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.eolmago.domain.entity.user.QUserPenalty;
import kr.eolmago.domain.entity.user.User;
import kr.eolmago.domain.entity.user.UserPenalty;
import kr.eolmago.domain.entity.user.enums.PenaltyType;
import kr.eolmago.domain.entity.user.enums.UserStatus;
import kr.eolmago.repository.user.UserPenaltyRepositoryCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static kr.eolmago.domain.entity.user.QUser.user;
import static kr.eolmago.domain.entity.user.QUserPenalty.userPenalty;

@Repository
@RequiredArgsConstructor
public class UserPenaltyRepositoryImpl implements UserPenaltyRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<UserPenalty> findExpiredPenalties(OffsetDateTime now) {
        return queryFactory
                .selectFrom(userPenalty)
                .join(userPenalty.user, user).fetchJoin()
                .where(
                        userPenalty.type.eq(PenaltyType.SUSPENDED),
                        userPenalty.expiresAt.isNotNull(),
                        userPenalty.expiresAt.before(now),
                        user.status.eq(UserStatus.SUSPENDED)
                )
                .fetch();
    }

    @Override
    public boolean existsActivePenalty(User targetUser, OffsetDateTime now) {
        Integer fetchOne = queryFactory
                .selectOne()
                .from(userPenalty)
                .where(
                        userPenalty.user.eq(targetUser),
                        userPenalty.expiresAt.gt(now).or(userPenalty.expiresAt.isNull())
                )
                .fetchFirst();

        return fetchOne != null;
    }

    @Override
    public Optional<UserPenalty> findActivePenaltyByUser(User user, OffsetDateTime now) {
        UserPenalty penalty = queryFactory
                .selectFrom(userPenalty)
                .where(
                        userPenalty.user.eq(user),
                        userPenalty.expiresAt.gt(now).or(userPenalty.expiresAt.isNull())
                )
                .orderBy(userPenalty.startedAt.desc())
                .fetchFirst();

        return Optional.ofNullable(penalty);
    }

    @Override
    public List<UserPenalty> findPenaltyHistoryByUser(User user) {
        QUserPenalty penalty = QUserPenalty.userPenalty;

        return queryFactory
                .selectFrom(penalty)
                .where(penalty.user.eq(user))
                .orderBy(penalty.startedAt.desc())
                .fetch();
    }
}
