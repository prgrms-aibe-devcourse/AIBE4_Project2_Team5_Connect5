package kr.eolmago.global.security.filter;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.eolmago.domain.entity.user.enums.UserStatus;
import kr.eolmago.global.security.CustomUserDetails;
import kr.eolmago.service.user.JwtService;
import kr.eolmago.service.user.RefreshTokenService;
import kr.eolmago.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String ACCESS_TOKEN_COOKIE = "accessToken";
    public static final String REFRESH_TOKEN_COOKIE = "refreshToken";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserService userService;
    private final RefreshTokenService refreshTokenService;

    private enum TokenStatus {
        VALID, EXPIRED, INVALID
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();

        return uri.startsWith("/css/")
                || uri.startsWith("/js/")
                || uri.startsWith("/images/")
                || uri.equals("/")
                || uri.equals("/home")
                || uri.equals("/login")
                || uri.startsWith("/auctions")
                || uri.startsWith("/oauth2/")
                || uri.startsWith("/login/oauth2/")
                || uri.startsWith("/swagger-ui/")
                || uri.equals("/swagger-ui.html")
                || uri.startsWith("/v3/api-docs/")
                || uri.startsWith("/api-docs/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String accessToken = extractAccessToken(request);
        String refreshToken = extractRefreshToken(request);

        // accessToken이 없는 경우 - refreshToken으로 갱신 시도
        if (accessToken == null || accessToken.isBlank()) {
            if (refreshToken != null && !refreshToken.isBlank()) {
                log.info("AccessToken 없음, RefreshToken으로 갱신 시도");
                if (tryRefreshTokens(refreshToken, request, response)) {
                    log.info("토큰 갱신 성공");
                } else {
                    log.warn("토큰 갱신 실패");
                    clearAuthCookies(response, request, "RefreshToken으로 갱신 실패 (accessToken 없음)");
                }
            } else {
                log.debug("토큰 없음, 인증 스킵");
            }
            filterChain.doFilter(request, response);
            return;
        }

        TokenStatus tokenStatus = checkTokenStatus(accessToken);

        switch (tokenStatus) {
            case VALID:
                authenticateUser(accessToken, request, response);
                break;

            case EXPIRED:
                log.info("AccessToken 만료, 갱신 시도");
                if (refreshToken != null && tryRefreshTokens(refreshToken, request, response)) {
                    log.info("토큰 갱신 성공");
                } else {
                    log.warn("토큰 갱신 실패, 재로그인 필요");
                    clearAuthCookies(response, request, "AccessToken 만료 후 RefreshToken 갱신 실패");
                }
                break;

            case INVALID:
                log.warn("AccessToken 유효하지 않음");
                SecurityContextHolder.clearContext();
                break;
        }

        filterChain.doFilter(request, response);
    }

    private TokenStatus checkTokenStatus(String token) {
        try {
            jwtService.parseToken(token);
            return TokenStatus.VALID;
        } catch (ExpiredJwtException e) {
            return TokenStatus.EXPIRED;
        } catch (Exception e) {
            return TokenStatus.INVALID;
        }
    }

    private boolean tryRefreshTokens(String refreshToken, HttpServletRequest request, HttpServletResponse response) {
        try {
            TokenStatus refreshStatus = checkTokenStatus(refreshToken);
            log.info("RefreshToken 상태: {}", refreshStatus);

            if (refreshStatus != TokenStatus.VALID) {
                log.warn("RefreshToken 유효하지 않음: {}", refreshStatus);
                return false;
            }

            UUID userId = jwtService.getUserIdFromToken(refreshToken);
            log.info("RefreshToken에서 userId 추출: {}", userId);

            if (!refreshTokenService.validate(userId, refreshToken)) {
                log.warn("RefreshToken이 Redis와 일치하지 않음");
                return false;
            }

            UserDetails userDetails = userService.getUserDetailsById(userId);

            String role = userDetails.getAuthorities().iterator().next()
                    .getAuthority().replace("ROLE_", "");

            String newAccessToken = jwtService.generateAccessToken(
                    userId,
                    userDetails.getUsername(),
                    role
            );

            // AccessToken은 항상 갱신
            setAccessTokenCookie(response, newAccessToken);

            // RefreshToken은 만료가 1일 미만으로 남았을 때만 갱신 (동시성 문제 최소화)
            long oneDayMillis = 24 * 60 * 60 * 1000L;
            long remainingMillis = jwtService.parseToken(refreshToken).getExpiration().getTime() - System.currentTimeMillis();

            if (remainingMillis < oneDayMillis) {
                String newRefreshToken = jwtService.generateRefreshToken(userId);
                refreshTokenService.rotate(userId, newRefreshToken);
                setRefreshTokenCookie(response, newRefreshToken);
                log.info("RefreshToken 갱신 (만료 임박): userId={}", userId);
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.info("토큰 갱신 완료: userId={}", userId);
            return true;

        } catch (Exception e) {
            log.error("토큰 갱신 중 오류: {}", e.getMessage(), e);
            return false;
        }
    }

    private void authenticateUser(String token, HttpServletRequest request, HttpServletResponse response) {
        try {
            UUID userId = jwtService.getUserIdFromToken(token);
            UserDetails userDetails = userService.getUserDetailsById(userId);

            // SUSPENDED 유저 체크
            if (userDetails instanceof CustomUserDetails) {
                CustomUserDetails customUserDetails = (CustomUserDetails) userDetails;
                if (UserStatus.SUSPENDED.name().equals(customUserDetails.getStatus())) {
                    String method = request.getMethod();
                    // GET 요청은 허용, 그 외(POST, PUT, DELETE 등)는 차단
                    if (!"GET".equalsIgnoreCase(method)) {
                        log.warn("SUSPENDED 사용자의 쓰기 요청 차단: userId={}, method={}", userId, method);
                        // SecurityContext에 인증 정보를 설정하지 않음 -> 401/403 처리됨
                        SecurityContextHolder.clearContext();
                        // AuthenticationEntryPoint에서 처리할 수 있도록 예외 정보를 request에 담거나
                        // 여기서 직접 예외를 던지면 FilterChain에서 잡히지 않을 수 있음.
                        // SecurityContext가 비어있으면 AuthenticationEntryPoint가 호출됨.
                        // 하지만 구체적인 이유(SUSPENDED)를 전달하기 위해 request attribute 활용 가능
                        request.setAttribute("SPRING_SECURITY_LAST_EXCEPTION", new InsufficientAuthenticationException("SUSPENDED"));
                        return;
                    }
                }
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("인증 성공: userId={}", userId);
        } catch (Exception e) {
            log.warn("인증 처리 중 예외: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }
    }

    private String extractAccessToken(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith(BEARER_PREFIX)) {
            return auth.substring(BEARER_PREFIX.length()).trim();
        }
        return extractCookieValue(request, ACCESS_TOKEN_COOKIE);
    }

    private String extractRefreshToken(HttpServletRequest request) {
        return extractCookieValue(request, REFRESH_TOKEN_COOKIE);
    }

    private String extractCookieValue(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;

        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private void setAccessTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie(ACCESS_TOKEN_COOKIE, token);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge((int) jwtService.getAccessTokenExpirySeconds());
        response.addCookie(cookie);
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE, token);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge((int) jwtService.getRefreshTokenExpirySeconds());
        response.addCookie(cookie);
    }

    private void clearAuthCookies(HttpServletResponse response,  HttpServletRequest request, String reason) {

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));

        Cookie accessCookie = new Cookie(ACCESS_TOKEN_COOKIE, null);
        accessCookie.setPath("/");
        accessCookie.setMaxAge(0);
        response.addCookie(accessCookie);

        Cookie refreshCookie = new Cookie(REFRESH_TOKEN_COOKIE, null);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(0);
        response.addCookie(refreshCookie);
    }
}