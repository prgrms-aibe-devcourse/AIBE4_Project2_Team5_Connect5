package kr.eolmago.controller.api.user;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import kr.eolmago.domain.entity.user.User;
import kr.eolmago.domain.entity.user.UserPenalty;
import kr.eolmago.dto.api.user.request.CheckNicknameRequest;
import kr.eolmago.dto.api.user.request.UpdateUserProfileRequest;
import kr.eolmago.dto.api.user.request.VerifyPhoneNumberRequest;
import kr.eolmago.dto.api.user.response.CheckNicknameResponse;
import kr.eolmago.dto.api.user.response.UserPenaltyInfoResponse;
import kr.eolmago.dto.api.user.response.UserProfileResponse;
import kr.eolmago.dto.api.user.response.VerifyPhoneNumberResponse;
import kr.eolmago.global.security.CustomUserDetails;
import kr.eolmago.service.user.UserPenaltyService;
import kr.eolmago.service.user.UserProfileService;
import kr.eolmago.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final UserPenaltyService userPenaltyService;
    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getId());
        try {
            UserProfileResponse response = userProfileService.getUserProfile(userId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/me/penalty")
    public ResponseEntity<UserPenaltyInfoResponse> getMyPenalty(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getId());
        try {
            User user = userService.getUserById(userId);
            Optional<UserPenalty> penalty = userPenaltyService.getActivePenalty(user);

            if (penalty.isPresent()) {
                UserPenalty p = penalty.get();
                UserPenaltyInfoResponse response = UserPenaltyInfoResponse.of(
                        p.getType().name(),
                        p.getReason(),
                        p.getExpiresAt()
                );
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
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

        try {
            UserProfileResponse response = userProfileService.updateUserProfile(
                    userId,
                    request,
                    image
            );
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
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
        try {
            boolean available = userProfileService.isNicknameAvailable(request.nickname());
            CheckNicknameResponse response = available
                    ? CheckNicknameResponse.available(request.nickname())
                    : CheckNicknameResponse.duplicate(request.nickname());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/sendPhoneVerification")
    public ResponseEntity<?> sendPhoneVerificationCode(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String phoneNumber
    ) {
        UUID userId = UUID.fromString(userDetails.getId());
        try {
            userProfileService.sendPhoneVerificationCode(userId, phoneNumber);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/verifyPhone")
    public ResponseEntity<?> verifyPhoneNumber(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody VerifyPhoneNumberRequest request
    ) {
        UUID userId = UUID.fromString(userDetails.getId());
        try {
            userProfileService.verifyPhoneNumber(userId, request.phoneNumber(), request.verificationCode());
            VerifyPhoneNumberResponse response = VerifyPhoneNumberResponse.success(request.phoneNumber());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
