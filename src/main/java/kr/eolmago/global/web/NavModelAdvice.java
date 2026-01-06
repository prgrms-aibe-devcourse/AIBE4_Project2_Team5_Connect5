package kr.eolmago.global.web;

import java.util.UUID;
import kr.eolmago.global.security.CustomUserDetails;
import kr.eolmago.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class NavModelAdvice {

	private final NotificationService notificationService;

	@ModelAttribute("isAuthenticated")
	public boolean isAuthenticated(@AuthenticationPrincipal CustomUserDetails me) {
		return tryUserId(me) != null;
	}

	@ModelAttribute("userName")
	public String userName(@AuthenticationPrincipal CustomUserDetails me) {
		return me != null ? me.getUsername() : null;
	}

	@ModelAttribute("unreadNotificationCount")
	public Long unreadNotificationCount(@AuthenticationPrincipal CustomUserDetails me) {
		UUID userId = tryUserId(me);
		if (userId == null) return 0L;
		return notificationService.unreadCount(userId);
	}

	private UUID tryUserId(CustomUserDetails me) {
		if (me == null || me.getId() == null || me.getId().isBlank()) return null;
		try {
			return UUID.fromString(me.getId());
		} catch (Exception e) {
			return null;
		}
	}
}
