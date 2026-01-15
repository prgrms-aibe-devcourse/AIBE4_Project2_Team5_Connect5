package kr.eolmago.repository.notification;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface NotificationRepositoryCustom {
    int markAllRead(UUID userId, OffsetDateTime now);
}
