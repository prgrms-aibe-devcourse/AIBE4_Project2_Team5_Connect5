package kr.eolmago.controller.api.user;

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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserProfileController {

    private final UserProfileService userProfileService;

    /**
     * 현재 사용자의 프로필 조회
     * GET /api/users/me
     */
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

    /**
     * 프로필 수정
     * PUT /api/users/me
     */
    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateUserProfileRequest request
    ) {
        UUID userId = UUID.fromString(userDetails.getId());
        log.info("프로필 수정 요청: userId={}", userId);

        try {
            UserProfileResponse response = userProfileService.updateUserProfile(userId, request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("프로필 수정 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("프로필 수정 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 닉네임 중복 체크
     * POST /api/users/checkNickname
     */
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

    // ToDo coolSMS 적용하면서 수정 예정
    /**
     * 핸드폰 인증 코드 발송
     * POST /api/users/sendPhoneVerification?phoneNumber=01012345678
     */
    @PostMapping("/sendPhoneVerification")
    public ResponseEntity<Void> sendPhoneVerificationCode(
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
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("핸드폰 인증 코드 발송 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 핸드폰 인증 코드 검증
     * POST /api/users/verifyPhone
     */
    @PostMapping("/verifyPhone")
    public ResponseEntity<VerifyPhoneNumberResponse> verifyPhoneNumber(
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
            VerifyPhoneNumberResponse response = VerifyPhoneNumberResponse.failure(request.phoneNumber());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("핸드폰 인증 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
