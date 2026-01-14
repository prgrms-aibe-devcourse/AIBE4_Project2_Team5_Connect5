package kr.eolmago.service.user;

import kr.eolmago.domain.entity.report.Report;
import kr.eolmago.domain.entity.report.enums.ReportAction;
import kr.eolmago.domain.entity.user.User;
import kr.eolmago.domain.entity.user.UserPenalty;
import kr.eolmago.domain.entity.user.enums.PenaltyType;
import kr.eolmago.domain.entity.user.enums.UserStatus;
import kr.eolmago.repository.user.UserPenaltyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserPenaltyService {

    private final UserPenaltyRepository userPenaltyRepository;

    public void applyPenalty(User user, Report report, ReportAction action, String reason) {
        // 경고나 조치 없음은 별도의 Penalty 엔티티를 생성하지 않음 (필요 시 수정 가능)
        if (action == ReportAction.NONE || action == ReportAction.WARN) {
            return;
        }

        PenaltyType penaltyType;
        OffsetDateTime expiresAt = null;
        OffsetDateTime now = OffsetDateTime.now();

        switch (action) {
            case SUSPEND_1D -> {
                penaltyType = PenaltyType.SUSPENDED;
                expiresAt = now.plusDays(1);
                user.updateStatus(UserStatus.SUSPENDED);
            }
            case SUSPEND_7D -> {
                penaltyType = PenaltyType.SUSPENDED;
                expiresAt = now.plusDays(7);
                user.updateStatus(UserStatus.SUSPENDED);
            }
            case BAN -> {
                penaltyType = PenaltyType.BANNED;
                expiresAt = null; // 무기한
                user.updateStatus(UserStatus.BANNED);
            }
            default -> throw new IllegalArgumentException("지원하지 않는 제재 유형입니다.");
        }

        UserPenalty penalty = UserPenalty.create(
                user,
                report,
                penaltyType,
                reason,
                now,
                expiresAt
        );

        userPenaltyRepository.save(penalty);
    }

    @Transactional(readOnly = true)
    public Optional<UserPenalty> getActivePenalty(User user) {
        OffsetDateTime now = OffsetDateTime.now();
        return userPenaltyRepository.findActivePenaltyByUser(user, now);
    }
}
