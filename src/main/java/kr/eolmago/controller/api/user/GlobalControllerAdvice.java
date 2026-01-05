package kr.eolmago.controller.api.user;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import kr.eolmago.global.security.CustomUserDetails;
import kr.eolmago.global.security.filter.JwtAuthenticationFilter;
import kr.eolmago.service.user.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalControllerAdvice {

    private final JwtService jwtService;

    /**
     * 모든 Thymeleaf 템플릿에 로그인 여부를 전달
     */
    @ModelAttribute("isAuthenticated")
    public boolean isAuthenticated(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return false;
        }

        for (Cookie cookie : cookies) {
            if (JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE.equals(cookie.getName())) {
                String token = cookie.getValue();
                if (token != null && !token.isEmpty()) {
                    try {
                        return jwtService.validateToken(token);
                    } catch (Exception e) {
                        log.debug("Token validation error", e);
                        return false;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 모든 Thymeleaf 템플릿에 사용자 이름을 전달
     */
    @ModelAttribute("userName")
    public String getUserName() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                    && authentication.getPrincipal() instanceof CustomUserDetails) {
                CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
                return userDetails.getUsername();
            }
        } catch (Exception e) {
            log.debug("Failed to get user name", e);
        }
        return null;
    }

    /**
     * 읽지 않은 채팅 수 (필요시 구현)
     */
    @ModelAttribute("unreadChatCount")
    public Integer getUnreadChatCount() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                    && authentication.getPrincipal() instanceof CustomUserDetails) {
                // TODO: 실제 읽지 않은 채팅 수를 조회하는 서비스 호출
                // CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
                // return chatService.getUnreadCount(userDetails.getId());
                return 0;
            }
        } catch (Exception e) {
            log.debug("Failed to get unread chat count", e);
        }
        return 0;
    }

    /**
     * 읽지 않은 알림 수 (필요시 구현)
     */
    @ModelAttribute("unreadNotificationCount")
    public Integer getUnreadNotificationCount() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                    && authentication.getPrincipal() instanceof CustomUserDetails) {
                // TODO: 실제 읽지 않은 알림 수를 조회하는 서비스 호출
                // CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
                // return notificationService.getUnreadCount(userDetails.getId());
                return 0;
            }
        } catch (Exception e) {
            log.debug("Failed to get unread notification count", e);
        }
        return 0;
    }
}