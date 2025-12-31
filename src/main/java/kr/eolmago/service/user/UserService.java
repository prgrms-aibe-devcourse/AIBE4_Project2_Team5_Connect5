package kr.eolmago.service.user;

import kr.eolmago.domain.entity.user.User;
import kr.eolmago.domain.entity.user.enums.UserRole;
import kr.eolmago.repository.user.SocialLoginRepository;
import kr.eolmago.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    public void updateUserRole(UUID adminId, UUID targetUserId, UserRole newRole) {
        User admin = getUserById(adminId);
        User targetUser = getUserById(targetUserId);

        targetUser.changeRole(admin, newRole);
        userRepository.save(targetUser);

        log.info("사용자 역할 변경: adminId={}, targetUserId={}, role={}",
                adminId, targetUserId, newRole);
    }

    public void suspendUser(UUID adminId, UUID targetUserId) {
        User admin = getUserById(adminId);
        User targetUser = getUserById(targetUserId);

        targetUser.suspend(admin);
        userRepository.save(targetUser);

        log.info("사용자 계정 정지: adminId={}, targetUserId={}", adminId, targetUserId);
    }

    public void banUser(UUID adminId, UUID targetUserId) {
        User admin = getUserById(adminId);
        User targetUser = getUserById(targetUserId);

        targetUser.ban(admin);
        userRepository.save(targetUser);

        log.info("사용자 계정 차단: adminId={}, targetUserId={}", adminId, targetUserId);
    }

    public void activateUser(UUID adminId, UUID targetUserId) {
        User admin = getUserById(adminId);
        User targetUser = getUserById(targetUserId);

        targetUser.activate(admin);
        userRepository.save(targetUser);

        log.info("사용자 계정 활성화: adminId={}, targetUserId={}", adminId, targetUserId);
    }

    public void autoActivateUser(UUID userId) {
        User user = getUserById(userId);

        user.autoActivate();
        userRepository.save(user);

        log.info("사용자 자동 활성화: userId={}", userId);
    }
}
