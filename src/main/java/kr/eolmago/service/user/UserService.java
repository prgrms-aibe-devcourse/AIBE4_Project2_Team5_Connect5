package kr.eolmago.service.user;

import kr.eolmago.domain.entity.user.SocialLogin;
import kr.eolmago.domain.entity.user.User;
import kr.eolmago.domain.entity.user.enums.UserRole;
import kr.eolmago.domain.entity.user.enums.UserStatus;
import kr.eolmago.global.security.CustomUserDetails;
import kr.eolmago.repository.user.SocialLoginRepository;
import kr.eolmago.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final SocialLoginRepository socialLoginRepository;

    @Transactional(readOnly = true)
    public User getUserById(UUID userId) {
        return userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. : " + userId));
    }

    @Transactional(readOnly = true)
    public UserDetails getUserDetailsById(UUID userId) {
        User user = getUserById(userId);
        SocialLogin socialLogin = socialLoginRepository.findByUser(user).stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("소셜 로그인 정보를 찾을 수 없습니다."));

        return CustomUserDetails.from(user, socialLogin);
    }

    private void validateAdminRole(User admin) {
        if (admin.getRole() != UserRole.ADMIN) {
            throw new IllegalArgumentException("관리자 계정이 아닙니다.");
        }
    }

    private void validateStatusNotAlready(User user, UserStatus status, String message) {
        if (user.getStatus() == status) {
            throw new IllegalArgumentException(message);
        }
    }

    public void updateUserRole(UUID adminId, UUID targetUserId, UserRole newRole) {
        User admin = getUserById(adminId);
        User targetUser = getUserById(targetUserId);

        validateAdminRole(admin);

        if (newRole == null) {
            throw new IllegalArgumentException("변경할 역할은 null이 될 수 없습니다.");
        }

        if (targetUser.getRole() == newRole) {
            throw new IllegalArgumentException("현재 역할과 동일한 역할로 변경할 수 없습니다.");
        }

        targetUser.updateRole(newRole);

        log.info("사용자 역할 변경: adminId={}, targetUserId={}, role={}",
                adminId, targetUserId, newRole);
    }

    public void suspendUser(UUID adminId, UUID targetUserId) {
        User admin = getUserById(adminId);
        User targetUser = getUserById(targetUserId);

        validateAdminRole(admin);
        validateStatusNotAlready(targetUser, UserStatus.SUSPENDED, "이미 정지된 사용자입니다.");

        targetUser.updateStatus(UserStatus.SUSPENDED);

        log.info("사용자 계정 정지: adminId={}, targetUserId={}", adminId, targetUserId);
    }

    public void banUser(UUID adminId, UUID targetUserId) {
        User admin = getUserById(adminId);
        User targetUser = getUserById(targetUserId);

        validateAdminRole(admin);
        validateStatusNotAlready(targetUser, UserStatus.BANNED, "이미 차단된 사용자입니다.");

        targetUser.updateStatus(UserStatus.BANNED);

        log.info("사용자 계정 차단: adminId={}, targetUserId={}", adminId, targetUserId);
    }

    public void activateUser(UUID adminId, UUID targetUserId) {
        User admin = getUserById(adminId);
        User targetUser = getUserById(targetUserId);

        validateAdminRole(admin);
        validateStatusNotAlready(targetUser, UserStatus.ACTIVE, "이미 활성화된 사용자입니다.");

        targetUser.updateStatus(UserStatus.ACTIVE);

        log.info("사용자 계정 활성화: adminId={}, targetUserId={}", adminId, targetUserId);
    }

    public void autoActivateUser(UUID userId) {
        User user = getUserById(userId);

        if (user.getStatus() == UserStatus.ACTIVE) {
            return;
        }

        user.updateStatus(UserStatus.ACTIVE);

        log.info("사용자 자동 활성화: userId={}", userId);
    }
}
