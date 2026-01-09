package kr.eolmago.service.user;

import kr.eolmago.domain.entity.user.User;
import kr.eolmago.domain.entity.user.UserProfile;
import kr.eolmago.domain.entity.user.enums.UserRole;
import kr.eolmago.dto.api.user.request.UpdateUserProfileRequest;
import kr.eolmago.dto.api.user.response.UserProfileResponse;
import kr.eolmago.repository.user.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final VerificationCodeService verificationCodeService;
    private final UserProfileImageUploadService userProfileImageUploadService;

    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(UUID userId) {
        log.debug("프로필 조회: userId={}", userId);
        UserProfile userProfile = userProfileRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new IllegalArgumentException("프로필을 찾을 수 없습니다"));
        return UserProfileResponse.from(userProfile);
    }

    public UserProfileResponse updateUserProfile(
            UUID userId,
            UpdateUserProfileRequest request,
            MultipartFile image
    ) {
        log.debug("프로필 수정: userId={}, hasImage={}", userId, image != null && !image.isEmpty());

        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            imageUrl = userProfileImageUploadService.uploadUserProfileImage(image, userId);
            log.info("이미지 업로드 완료: imageUrl={}", imageUrl);
        }

        return updateProfileInTransaction(userId, request, imageUrl);
    }

    @Transactional
    public UserProfileResponse updateProfileInTransaction(
            UUID userId,
            UpdateUserProfileRequest request,
            String newImageUrl
    ) {
        UserProfile userProfile = userProfileRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new IllegalArgumentException("프로필을 찾을 수 없습니다"));

        if (!request.nickname().equals(userProfile.getNickname())) {
            if (userProfileRepository.existsByNickname(request.nickname())) {
                throw new IllegalArgumentException("이미 사용 중인 닉네임입니다");
            }
        }

        String finalImageUrl = (newImageUrl != null) ? newImageUrl : userProfile.getProfileImageUrl();

        userProfile.updateProfile(
                request.name(),
                request.nickname(),
                request.phoneNumber(),
                finalImageUrl
        );

        userProfileRepository.save(userProfile);
        log.info("프로필 DB 업데이트 완료: userId={}", userId);

        return UserProfileResponse.from(userProfile);
    }

    @Transactional(readOnly = true)
    public boolean isNicknameAvailable(String nickname) {
        boolean exists = userProfileRepository.existsByNickname(nickname);
        log.debug("닉네임 사용 가능 여부: nickname={}, available={}", nickname, !exists);
        return !exists;
    }

    @Transactional
    public void sendPhoneVerificationCode(UUID userId, String phoneNumber) {
        log.debug("핸드폰 인증 코드 발송: userId={}, phoneNumber={}", userId, phoneNumber);
        userProfileRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new IllegalArgumentException("프로필을 찾을 수 없습니다"));

        if (!phoneNumber.matches("^\\d{10,11}$")) {
            throw new IllegalArgumentException("유효한 핸드폰 번호가 아닙니다");
        }

        verificationCodeService.generateAndSendVerificationCode(phoneNumber);
        log.info("인증 코드 발송 완료: userId={}, phoneNumber={}", userId, phoneNumber);
    }

    @Transactional
    public void verifyPhoneNumber(UUID userId, String phoneNumber, String verificationCode) {
        log.debug("핸드폰 인증: userId={}, phoneNumber={}", userId, phoneNumber);
        UserProfile userProfile = userProfileRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new IllegalArgumentException("프로필을 찾을 수 없습니다"));

        if (!verificationCodeService.verifyCode(phoneNumber, verificationCode)) {
            throw new IllegalArgumentException("인증 코드가 일치하지 않습니다");
        }

        userProfile.updateProfile(
                userProfile.getName(),
                userProfile.getNickname(),
                phoneNumber,
                userProfile.getProfileImageUrl()
        );
        userProfile.verifyPhoneNumber();
        userProfileRepository.save(userProfile);

        User user = userProfile.getUser();
        if (user.getRole() == UserRole.GUEST) {
            user.updateRole(UserRole.USER);
            log.info("사용자 역할 변경: userId={}, newRole=USER", userId);
        }

        verificationCodeService.deleteVerificationCode(phoneNumber);
        log.info("핸드폰 인증 완료: userId={}, phoneNumber={}", userId, phoneNumber);
    }
}
