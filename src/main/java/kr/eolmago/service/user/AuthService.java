package kr.eolmago.service.user;

import kr.eolmago.domain.entity.user.SocialLogin;
import kr.eolmago.domain.entity.user.User;
import kr.eolmago.dto.api.user.request.LoginRequest;
import kr.eolmago.dto.api.user.response.TokenResponse;
import kr.eolmago.repository.user.SocialLoginRepository;
import kr.eolmago.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {
    private final SocialLoginRepository socialLoginRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        SocialLogin socialLoginUser = socialLoginRepository.findByEmail(request.email())
                .stream().findFirst()
                .orElseThrow(() -> new BadCredentialsException("이메일 오류"));

        User user = socialLoginUser.getUser();

        String accessToken = jwtService.generateAccessToken(
                user.getUserId(), socialLoginUser.getEmail(), user.getRole().name()
        );
        String refreshToken = jwtService.generateRefreshToken(user.getUserId());
        refreshTokenService.save(user.getUserId(), refreshToken);

        log.info("로그인 성공 : {}", socialLoginUser.getEmail());

        return new TokenResponse(
                accessToken,
                refreshToken,
                "Bearer",
                jwtService.getAccessTokenExpirySeconds()
        );
    }

    @Transactional(readOnly = true)
    public TokenResponse refresh(String refreshToken) {
        if (!jwtService.validateToken(refreshToken)) {
            throw new BadCredentialsException("유효하지 않은 Refresh Token입니다.");
        }

        UUID userId = jwtService.getUserIdFromToken(refreshToken);

        if (!refreshTokenService.validate(userId, refreshToken)) {
            throw new BadCredentialsException("Refresh Token이 유효하지 않거나 만료됐습니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("사용자 없음"));
        
        SocialLogin socialLogin = socialLoginRepository.findByUser(user).stream().findFirst()
                .orElseThrow(() -> new BadCredentialsException("소셜 로그인 정보 없음"));

        String newAccessToken = jwtService.generateAccessToken(
                user.getUserId(), socialLogin.getEmail(), user.getRole().name());
        String newRefreshToken = jwtService.generateRefreshToken(user.getUserId());
        refreshTokenService.update(user.getUserId(), newRefreshToken);

        return new TokenResponse(
                newAccessToken,
                newRefreshToken,
                "Bearer",
                jwtService.getAccessTokenExpirySeconds()
        );
    }

    public void logout(UUID userId) {
        refreshTokenService.delete(userId);
        log.info("로그아웃 : userId={}", userId);
    }
}
