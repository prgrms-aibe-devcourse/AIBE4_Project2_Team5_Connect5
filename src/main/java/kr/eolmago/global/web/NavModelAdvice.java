package kr.eolmago.global.web;

import kr.eolmago.global.security.CustomUserDetails;
import kr.eolmago.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.UUID;

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

	@ModelAttribute("userProfileImage")
	public String userProfileImage(@AuthenticationPrincipal CustomUserDetails me) {
		if (me != null && me.getProfileImageUrl() != null) {
			return me.getProfileImageUrl();
		}
		return "/images/profile/base.png"; // 기본 이미지
	}

	@ModelAttribute("unreadNotificationCount")
	public Long unreadNotificationCount(@AuthenticationPrincipal CustomUserDetails me) {
		UUID userId = tryUserId(me);
		if (userId == null) return 0L;
		return notificationService.unreadCount(userId);
	}

    @ModelAttribute("userRole")
    public String userRole(@AuthenticationPrincipal CustomUserDetails me) {
        if (me == null) return null;
        return me.getAuthorities().stream()
                .findFirst()
                .map(authority -> authority.getAuthority().replace("ROLE_", ""))
                .orElse(null);
    }

    @ModelAttribute("userStatus")
    public String userStatus(@AuthenticationPrincipal CustomUserDetails me) {
        if (me == null) return null;
        return me.getStatus(); // CustomUserDetails에 getStatus() 메서드가 있다고 가정
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
