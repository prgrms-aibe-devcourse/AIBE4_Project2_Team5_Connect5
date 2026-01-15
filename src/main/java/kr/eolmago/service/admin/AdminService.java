package kr.eolmago.service.admin;

import kr.eolmago.domain.entity.report.Report;
import kr.eolmago.domain.entity.report.enums.ReportAction;
import kr.eolmago.domain.entity.report.enums.ReportReason;
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
     * 신고 상세 조회 (조회 시 PENDING → UNDER_REVIEW)
     */
    @Transactional
    public ReportAdminResponse getReportDetail(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 신고입니다."));

        // 처리 대기 상태면 → 처리중으로 변경
        if (report.getStatus() == ReportStatus.PENDING) {
            report.updateStatus(ReportStatus.UNDER_REVIEW);
            log.info("신고 상태 변경: reportId={}, PENDING → UNDER_REVIEW", reportId);
        }

        return toReportAdminResponse(report);
    }

    /**
     * 신고 처리 (제재 조치 + 상태 변경)
     */
    @Transactional
    public void resolveReport(Long reportId, ReportAction action, String adminNote) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 신고입니다."));

        // 이미 처리 완료된 신고는 재처리 불가
        if (report.getStatus() == ReportStatus.RESOLVED || report.getStatus() == ReportStatus.REJECTED) {
            throw new IllegalStateException("이미 처리된 신고입니다.");
        }

        User reportedUser = report.getReportedUser();
        String reportReasonText = getReportReasonText(report.getReason());

        // ReportAction에 따른 제재 처리
        switch (action) {
            case NONE:
                // 조치 없음 → REJECTED
                report.updateAction(action);
                report.updateStatus(ReportStatus.REJECTED);
                report.updateResolvedAt(OffsetDateTime.now());
                if (adminNote != null) {
                    report.UpdateActionMemo(adminNote);
                }
                log.info("신고 처리 (조치 없음): reportId={}, action={}", reportId, action);
                break;

            case SUSPEND_1D:
                applySuspension(reportedUser, 1, reportReasonText, report);
                report.updateAction(action);
                report.updateStatus(ReportStatus.RESOLVED);
                report.updateResolvedAt(OffsetDateTime.now());
                if (adminNote != null) {
                    report.UpdateActionMemo(adminNote);
                }
                log.info("신고 처리 (1일 정지): reportId={}, userId={}", reportId, reportedUser.getUserId());
                break;

            case SUSPEND_7D:
                applySuspension(reportedUser, 7, reportReasonText, report);
                report.updateAction(action);
                report.updateStatus(ReportStatus.RESOLVED);
                report.updateResolvedAt(OffsetDateTime.now());
                if (adminNote != null) {
                    report.UpdateActionMemo(adminNote);
                }
                log.info("신고 처리 (7일 정지): reportId={}, userId={}", reportId, reportedUser.getUserId());
                break;

            case BAN:
                applyBan(reportedUser, reportReasonText, report);
                report.updateAction(action);
                report.updateStatus(ReportStatus.RESOLVED);
                report.updateResolvedAt(OffsetDateTime.now());
                if (adminNote != null) {
                    report.UpdateActionMemo(adminNote);
                }
                log.info("신고 처리 (영구 차단): reportId={}, userId={}", reportId, reportedUser.getUserId());
                break;

            default:
                throw new IllegalArgumentException("알 수 없는 조치입니다: " + action);
        }
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

    private void applySuspension(User user, int days, String reason, Report report) {
        // 이미 BANNED 상태면 변경 불가
        if (user.getStatus() == UserStatus.BANNED) {
            throw new IllegalStateException("이미 영구 차단된 사용자입니다.");
        }

        user.updateStatus(UserStatus.SUSPENDED);

        UserPenalty penalty = UserPenalty.create(
                user,
                report,
                PenaltyType.SUSPENDED,
                reason,
                OffsetDateTime.now(),
                OffsetDateTime.now().plusDays(days)
        );
        userPenaltyRepository.save(penalty);

        log.info("사용자 정지 처리: userId={}, days={}", user.getUserId(), days);
    }

    private void applyBan(User user, String reason, Report report) {
        user.updateStatus(UserStatus.BANNED);

        UserPenalty penalty = UserPenalty.create(
                user,
                report,
                PenaltyType.BANNED,
                reason,
                OffsetDateTime.now(),
                null // 영구 차단
        );
        userPenaltyRepository.save(penalty);

        log.info("사용자 영구 차단 처리: userId={}", user.getUserId());
    }

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

    /**
     * ReportReason enum을 한글 설명으로 변환
     */
    private String getReportReasonText(ReportReason reason) {
        return switch (reason) {
            case FRAUD_SUSPECT -> "사기 의심";
            case ITEM_NOT_AS_DESCRIBED -> "설명/사진 불일치";
            case ABUSIVE_LANGUAGE -> "욕설/비매너";
            case SPAM_AD -> "광고/도배";
            case ILLEGAL_ITEM -> "불법/금지 품목";
            case COUNTERFEIT -> "가품 의심";
            case PERSONAL_INFO -> "개인정보 노출";
            case OTHER -> "기타";
        };
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

        UserProfile reporterProfile = reporter.getUserProfile();
        UserProfile reportedUserProfile = reportedUser.getUserProfile();

        String reporterNickname = reporterProfile != null
                ? reporterProfile.getNickname()
                : "탈퇴한 사용자";

        String reporterProfileImage = reporterProfile != null
                ? reporterProfile.getProfileImageUrl()
                : null;

        String reportedUserNickname = reportedUserProfile != null
                ? reportedUserProfile.getNickname()
                : "알 수 없음";

        String reportedUserProfileImage = reportedUserProfile != null
                ? reportedUserProfile.getProfileImageUrl()
                : null;

        return ReportAdminResponse.builder()
                .reportId(report.getReportId())
                .reportedUserId(reportedUser.getUserId())
                .reportedUserNickname(reportedUserNickname)
                .reportedUserProfileImage(reportedUserProfileImage)
                .reporterUserId(reporter.getUserId())
                .reporterNickname(reporterNickname)
                .reporterProfileImage(reporterProfileImage)
                .auctionId(report.getAuction() != null ? report.getAuction().getAuctionId() : null)
                .reason(report.getReason())
                .description(report.getDescription())
                .status(report.getStatus())
                .action(report.getAction())
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
