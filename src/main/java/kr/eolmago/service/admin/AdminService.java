package kr.eolmago.service.admin;

import kr.eolmago.domain.entity.report.Report;
import kr.eolmago.domain.entity.report.enums.ReportStatus;
import kr.eolmago.domain.entity.user.SocialLogin;
import kr.eolmago.domain.entity.user.User;
import kr.eolmago.domain.entity.user.UserPenalty;
import kr.eolmago.domain.entity.user.UserProfile;
import kr.eolmago.domain.entity.user.enums.PenaltyType;
import kr.eolmago.domain.entity.user.enums.UserRole;
import kr.eolmago.domain.entity.user.enums.UserStatus;
import kr.eolmago.dto.api.admin.response.PenaltyHistoryResponse;
import kr.eolmago.dto.api.admin.response.ReportAdminResponse;
import kr.eolmago.dto.api.admin.response.UserAdminResponse;
import kr.eolmago.dto.api.common.PageResponse;
import kr.eolmago.repository.report.ReportRepository;
import kr.eolmago.repository.user.SocialLoginRepository;
import kr.eolmago.repository.user.UserPenaltyRepository;
import kr.eolmago.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AdminService {

    private final UserRepository userRepository;
    private final UserPenaltyRepository userPenaltyRepository;
    private final SocialLoginRepository socialLoginRepository;
    private final ReportRepository reportRepository;

    /**
     * 사용자 목록 조회 (필터링 + 페이지네이션)
     */
    public PageResponse<UserAdminResponse> getUsers(String name, String email, UserStatus status, Pageable pageable) {
        Page<User> userPage = userRepository.findUsersWithFilters(name, email, status, pageable);
        return PageResponse.of(userPage, this::toUserAdminResponse);
    }

    /**
     * 신고 목록 조회 (필터링 + 페이지네이션)
     */
    public PageResponse<ReportAdminResponse> getReports(ReportStatus status, Pageable pageable) {
        Page<Report> reportPage = reportRepository.findReportsWithFilters(status, pageable);
        return PageResponse.of(reportPage, this::toReportAdminResponse);
    }

    /**
     * 사용자 상태 변경 (검증 로직 포함)
     */
    @Transactional
    public void updateUserStatus(UUID userId, UserStatus newStatus, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        UserStatus oldStatus = user.getStatus();
        if (oldStatus == newStatus) {
            throw new IllegalStateException("이미 해당 상태입니다.");
        }
        if (user.getRole() == UserRole.ADMIN) {
            throw new IllegalStateException("관리자 계정의 상태는 변경할 수 없습니다.");
        }

        user.updateStatus(newStatus);

        if (newStatus == UserStatus.SUSPENDED || newStatus == UserStatus.BANNED) {
            createPenaltyRecord(user, newStatus, reason);
        }

        log.info("관리자가 사용자 상태 변경: userId={}, {} -> {}, reason={}", userId, oldStatus, newStatus, reason);
    }

    /**
     * 전체 제재 이력 조회 (페이지네이션 + 필터링)
     */
    public PageResponse<PenaltyHistoryResponse> getAllPenalties(PenaltyType type, Pageable pageable) {
        Page<UserPenalty> penaltyPage = userPenaltyRepository.findAllPenaltiesWithFilters(type, pageable);
        return PageResponse.of(penaltyPage, this::toPenaltyHistoryResponseWithUser);
    }

    /**
     * 특정 유저의 제재 이력 조회
     */
    public List<PenaltyHistoryResponse> getPenaltyHistory(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        List<UserPenalty> penalties = userPenaltyRepository.findPenaltyHistoryByUser(user);
        return penalties.stream()
                .map(this::toPenaltyHistoryResponseWithUser)
                .toList();
    }

    // ================= PRIVATE METHODS ================= //

    private void createPenaltyRecord(User user, UserStatus status, String reason) {
        if (reason == null || reason.isBlank()) {
            reason = "관리자 직접 조치";
        }

        PenaltyType penaltyType;
        OffsetDateTime expiresAt;

        if (status == UserStatus.SUSPENDED) {
            penaltyType = PenaltyType.SUSPENDED;
            expiresAt = OffsetDateTime.now().plusDays(7);
        } else {
            penaltyType = PenaltyType.BANNED;
            expiresAt = null;
        }

        UserPenalty penalty = UserPenalty.create(user, null, penaltyType, reason, OffsetDateTime.now(), expiresAt);
        userPenaltyRepository.save(penalty);
    }

    private UserAdminResponse toUserAdminResponse(User user) {
        UserProfile profile = user.getUserProfile();
        String email = socialLoginRepository.findByUser(user).stream()
                .findFirst()
                .map(SocialLogin::getEmail)
                .orElse("이메일 없음");

        return UserAdminResponse.builder()
                .userId(user.getUserId())
                .nickname(profile != null ? profile.getNickname() : "알 수 없음")
                .email(email)
                .phone(profile != null ? profile.getPhoneNumber() : null)
                .status(user.getStatus())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .profileImageUrl(profile != null ? profile.getProfileImageUrl() : null)
                .build();
    }

    private ReportAdminResponse toReportAdminResponse(Report report) {
        User reporter = report.getReporter();
        User reportedUser = report.getReportedUser();

        String reporterNickname = Optional.ofNullable(reporter.getUserProfile())
                .map(UserProfile::getNickname)
                .orElse("탈퇴한 사용자");

        String reportedUserNickname = Optional.ofNullable(reportedUser.getUserProfile())
                .map(UserProfile::getNickname)
                .orElse("알 수 없음");

        return ReportAdminResponse.builder()
                .reportId(report.getReportId())
                .reportedUserId(reportedUser.getUserId())
                .reportedUserNickname(reportedUserNickname)
                .reporterUserId(reporter.getUserId())
                .reporterNickname(reporterNickname)
                .auctionId(report.getAuction().getAuctionId())
                .reason(report.getReason())
                .description(report.getDescription())
                .status(report.getStatus())
                .createdAt(report.getCreatedAt())
                .build();
    }

    private PenaltyHistoryResponse toPenaltyHistoryResponseWithUser(UserPenalty penalty) {
        User user = penalty.getUser();
        UserProfile profile = user.getUserProfile();

        return PenaltyHistoryResponse.builder()
                .penaltyId(penalty.getPenaltyId())
                .type(penalty.getType())
                .reason(penalty.getReason())
                .startedAt(penalty.getStartedAt())
                .expiresAt(penalty.getExpiresAt())
                .isActive(isActivePenalty(penalty))
                .userId(user.getUserId())
                .nickname(profile != null ? profile.getNickname() : "알 수 없음")
                .profileImageUrl(profile != null ? profile.getProfileImageUrl() : null)
                .build();
    }

    private boolean isActivePenalty(UserPenalty penalty) {
        if (penalty.getExpiresAt() == null) {
            return true; // 영구 정지
        }
        return penalty.getExpiresAt().isAfter(OffsetDateTime.now());
    }
}
