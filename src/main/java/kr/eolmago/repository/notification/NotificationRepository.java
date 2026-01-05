package kr.eolmago.repository.notification;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import kr.eolmago.domain.entity.notification.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

	Page<Notification> findByUser_UserIdAndDeletedFalseOrderByCreatedAtDesc(UUID userId, Pageable pageable);

	Optional<Notification> findByNotificationIdAndUser_UserIdAndDeletedFalse(Long notificationId, UUID userId);

	long countByUser_UserIdAndReadFalseAndDeletedFalse(UUID userId);

	@Modifying(clearAutomatically = true)
	@Query("""
        update Notification n
           set n.read = true, n.readAt = :now
         where n.user.userId = :userId
           and n.deleted = false
           and n.read = false
    """)
	int markAllRead(@Param("userId") UUID userId, @Param("now") OffsetDateTime now);
}
