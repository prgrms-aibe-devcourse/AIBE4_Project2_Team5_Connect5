package kr.eolmago.service.user;

import kr.eolmago.domain.entity.user.User;
import kr.eolmago.domain.entity.user.UserPenalty;
import kr.eolmago.domain.entity.user.enums.UserStatus;
import kr.eolmago.repository.user.UserPenaltyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserPenaltyScheduler {

    private final UserPenaltyRepository userPenaltyRepository;

    @Scheduled(cron = "0 0 0 * * *") // 매일 자정에 실행
    @Transactional
    public void releaseSuspendedUsers() {
        log.info("정지 해제 스케줄러 시작");
        OffsetDateTime now = OffsetDateTime.now();

        // 만료 시간이 지났고, 현재 상태가 SUSPENDED인 유저의 페널티 조회 (QueryDSL 사용)
        List<UserPenalty> expiredPenalties = userPenaltyRepository.findExpiredPenalties(now);

        for (UserPenalty penalty : expiredPenalties) {
            User user = penalty.getUser();
            
            // 해당 유저에게 아직 유효한 다른 정지 페널티가 있는지 확인 (QueryDSL 사용)
            boolean hasActivePenalty = userPenaltyRepository.existsActivePenalty(user, now);

            if (!hasActivePenalty) {
                user.updateStatus(UserStatus.ACTIVE);
                log.info("유저 {} 정지 해제 완료", user.getUserId());
            }
        }
        log.info("정지 해제 스케줄러 종료. 처리된 유저 수: {}", expiredPenalties.size());
    }
}
