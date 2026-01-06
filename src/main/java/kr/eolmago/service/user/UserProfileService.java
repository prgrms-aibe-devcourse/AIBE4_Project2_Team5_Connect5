package kr.eolmago.service.user;

import kr.eolmago.domain.entity.user.UserProfile;
import kr.eolmago.dto.api.user.request.UpdateUserProfileRequest;
import kr.eolmago.dto.api.user.response.UserProfileResponse;
import kr.eolmago.repository.user.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;

    /**
     * 사용자 프로필 조회
     * QueryDSL: 페치 조인으로 User 함께 로드 (N+1 해결)
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(UUID userId) {
        log.debug("프로필 조회: userId={}", userId);

        UserProfile userProfile = userProfileRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new IllegalArgumentException("프로필을 찾을 수 없습니다"));

        return UserProfileResponse.from(userProfile);
    }

    /**
     * 사용자 프로필 수정
     * 닉네임 중복 체크는 QueryDSL로 수행
     */
    public UserProfileResponse updateUserProfile(
            UUID userId,
            UpdateUserProfileRequest request
    ) {
        log.debug("프로필 수정: userId={}", userId);

        UserProfile userProfile = userProfileRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new IllegalArgumentException("프로필을 찾을 수 없습니다"));

        // 닉네임 중복 체크 (변경되는 경우만)
        if (!request.nickname().equals(userProfile.getNickname())) {
            if (userProfileRepository.existsByNickname(request.nickname())) {
                throw new IllegalArgumentException("이미 사용 중인 닉네임입니다");
            }
        }

        userProfile.updateProfile(
                request.name(),
                request.nickname(),
                request.phoneNumber(),
                request.profileImageUrl()
        );

        userProfileRepository.save(userProfile);

        log.info("프로필 수정 완료: userId={}", userId);

        return UserProfileResponse.from(userProfile);
    }

    /**
     * 닉네임 중복 체크
     * QueryDSL: existsByNickname 사용
     */
    @Transactional(readOnly = true)
    public boolean isNicknameAvailable(String nickname) {
        boolean exists = userProfileRepository.existsByNickname(nickname);
        log.debug("닉네임 사용 가능 여부: nickname={}, available={}", nickname, !exists);
        return !exists;
    }

    /**
     * 핸드폰 인증 코드 발송
     * TODO: CoolSMS 구현
     */
    public void sendPhoneVerificationCode(UUID userId, String phoneNumber) {
        log.debug("핸드폰 인증 코드 발송 기능은 테스트를 위해 임시로 비활성화되었습니다.");
        // log.debug("핸드폰 인증 코드 발송: userId={}, phoneNumber={}", userId, phoneNumber);

        // UserProfile userProfile = userProfileRepository.findByUserIdWithUser(userId)
        //         .orElseThrow(() -> new IllegalArgumentException("프로필을 찾을 수 없습니다"));

        // // 핸드폰 번호 유효성 검사
        // if (!phoneNumber.matches("^\\d{10,11}$")) {
        //     throw new IllegalArgumentException("유효한 핸드폰 번호가 아닙니다");
        // }

        // // TODO: CoolSMS를 통해 인증 코드 발송
        // // SmsService.sendVerificationCode(phoneNumber)를 호출할 예정

        // log.info("인증 코드 발송 완료: userId={}, phoneNumber={}", userId, phoneNumber);
    }

    /**
     * 핸드폰 인증 코드 검증
     */
    public void verifyPhoneNumber(UUID userId, String phoneNumber, String verificationCode) {
        log.debug("핸드폰 인증 기능은 테스트를 위해 임시로 비활성화되었습니다.");
        // log.debug("핸드폰 인증: userId={}, phoneNumber={}", userId, phoneNumber);

        // UserProfile userProfile = userProfileRepository.findByUserIdWithUser(userId)
        //         .orElseThrow(() -> new IllegalArgumentException("프로필을 찾을 수 없습니다"));

        // // 인증 코드 검증 (Redis에서 조회)
        // if (!verificationCodeService.verifyCode(phoneNumber, verificationCode)) {
        //     throw new IllegalArgumentException("인증 코드가 일치하지 않습니다");
        // }

        // // 프로필 업데이트 (핸드폰 번호 + 인증 완료)
        // userProfile.updateProfile(
        //         userProfile.getName(),
        //         userProfile.getNickname(),
        //         phoneNumber,
        //         userProfile.getProfileImageUrl()
        // );
        // userProfile.verifyPhoneNumber();

        // userProfileRepository.save(userProfile);

        // // Redis에서 인증 코드 삭제
        // verificationCodeService.deleteVerificationCode(phoneNumber);

        // log.info("핸드폰 인증 완료: userId={}, phoneNumber={}", userId, phoneNumber);
    }
}
