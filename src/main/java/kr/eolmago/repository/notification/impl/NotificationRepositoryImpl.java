package kr.eolmago.repository.notification.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import kr.eolmago.domain.entity.notification.QNotification;
import kr.eolmago.repository.notification.NotificationRepositoryCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class NotificationRepositoryImpl implements NotificationRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final EntityManager em;

    @Override
    public int markAllRead(UUID userId, OffsetDateTime now) {
        QNotification n = QNotification.notification;

        int updated = (int) queryFactory
                .update(n)
                .set(n.read, true)
                .set(n.readAt, now)
                .where(
                        n.user.userId.eq(userId),
                        n.deleted.isFalse(),
                        n.read.isFalse()
                )
                .execute();

        em.clear();

        return updated;
    }
}
