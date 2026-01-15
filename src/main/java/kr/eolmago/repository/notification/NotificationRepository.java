package kr.eolmago.repository.notification;

import java.util.Optional;
import java.util.UUID;

import kr.eolmago.domain.entity.notification.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;

public interface NotificationRepository extends JpaRepository<Notification, Long>, NotificationRepositoryCustom {

	Page<Notification> findByUser_UserIdAndDeletedFalseOrderByCreatedAtDesc(UUID userId, Pageable pageable);

	long countByUser_UserIdAndReadFalseAndDeletedFalse(UUID userId);

	Optional<Notification> findByNotificationIdAndUser_UserIdAndDeletedFalse(Long notificationId, UUID userId);
}
