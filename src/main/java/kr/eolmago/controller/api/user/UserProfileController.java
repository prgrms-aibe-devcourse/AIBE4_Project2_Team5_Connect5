package kr.eolmago.controller.api.user;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import kr.eolmago.dto.api.user.request.CheckNicknameRequest;
import kr.eolmago.dto.api.user.request.UpdateUserProfileRequest;
import kr.eolmago.dto.api.user.request.VerifyPhoneNumberRequest;
import kr.eolmago.dto.api.user.response.CheckNicknameResponse;
import kr.eolmago.dto.api.user.response.UserProfileResponse;
import kr.eolmago.dto.api.user.response.VerifyPhoneNumberResponse;
import kr.eolmago.global.security.CustomUserDetails;
import kr.eolmago.service.user.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getId());
        log.info("프로필 조회 요청: userId={}", userId);
        try {
            UserProfileResponse response = userProfileService.getUserProfile(userId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("프로필 조회 실패: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("프로필 조회 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestPart("data") @Valid UpdateUserProfileRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        UUID userId = UUID.fromString(userDetails.getId());
        log.info("프로필 수정 요청: userId={}, hasImage={}", userId, image != null && !image.isEmpty());

        try {
            UserProfileResponse response = userProfileService.updateUserProfile(
                    userId,
                    request,
                    image
            );
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("프로필 수정 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("프로필 수정 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String extractAccessToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring("Bearer ".length());
        }
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("accessToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    @PostMapping("/checkNickname")
    public ResponseEntity<CheckNicknameResponse> checkNickname(
            @Valid @RequestBody CheckNicknameRequest request
    ) {
        log.info("닉네임 중복 체크 요청: nickname={}", request.nickname());
        try {
            boolean available = userProfileService.isNicknameAvailable(request.nickname());
            CheckNicknameResponse response = available
                    ? CheckNicknameResponse.available(request.nickname())
                    : CheckNicknameResponse.duplicate(request.nickname());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("닉네임 중복 체크 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/sendPhoneVerification")
    public ResponseEntity<?> sendPhoneVerificationCode(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String phoneNumber
    ) {
        UUID userId = UUID.fromString(userDetails.getId());
        log.info("핸드폰 인증 코드 발송 요청: userId={}, phoneNumber={}", userId, phoneNumber);
        try {
            userProfileService.sendPhoneVerificationCode(userId, phoneNumber);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("핸드폰 인증 코드 발송 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("핸드폰 인증 코드 발송 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/verifyPhone")
    public ResponseEntity<?> verifyPhoneNumber(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody VerifyPhoneNumberRequest request
    ) {
        UUID userId = UUID.fromString(userDetails.getId());
        log.info("핸드폰 인증 요청: userId={}, phoneNumber={}", userId, request.phoneNumber());
        try {
            userProfileService.verifyPhoneNumber(userId, request.phoneNumber(), request.verificationCode());
            VerifyPhoneNumberResponse response = VerifyPhoneNumberResponse.success(request.phoneNumber());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("핸드폰 인증 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("핸드폰 인증 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
