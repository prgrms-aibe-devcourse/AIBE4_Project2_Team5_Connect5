package kr.eolmago.repository.user.impl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.eolmago.domain.entity.user.*;
import kr.eolmago.domain.entity.user.enums.PenaltyType;
import kr.eolmago.domain.entity.user.enums.UserStatus;
import kr.eolmago.repository.user.UserPenaltyRepositoryCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
        return queryFactory
                .selectFrom(userPenalty)
                .where(userPenalty.user.eq(user))
                .orderBy(userPenalty.startedAt.desc())
                .fetch();
    }

    // 전체 제재 이력 조회 (페이지네이션 + 필터링)
    @Override
    public Page<UserPenalty> findAllPenaltiesWithFilters(PenaltyType type, Pageable pageable) {
        QUserPenalty penalty = QUserPenalty.userPenalty;
        QUser user = QUser.user;
        QUserProfile profile = QUserProfile.userProfile;

        BooleanBuilder builder = new BooleanBuilder();

        // 필터: 제재 타입
        if (type != null) {
            builder.and(penalty.type.eq(type));
        }

        // 총 개수 조회
        Long total = queryFactory
                .select(penalty.count())
                .from(penalty)
                .where(builder)
                .fetchOne();

        // 데이터 조회 (fetchJoin으로 N+1 문제 방지)
        List<UserPenalty> penalties = queryFactory
                .selectFrom(penalty)
                .join(penalty.user, user).fetchJoin()
                .leftJoin(user.userProfile, profile).fetchJoin()
                .where(builder)
                .orderBy(penalty.startedAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        return new PageImpl<>(penalties, pageable, total != null ? total : 0);
    }
}
