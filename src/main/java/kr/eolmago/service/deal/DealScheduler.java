package kr.eolmago.service.deal;

import kr.eolmago.domain.entity.deal.Deal;
import kr.eolmago.domain.entity.deal.enums.DealStatus;
import kr.eolmago.repository.deal.DealRepository;
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
public class DealScheduler {

    private final DealRepository dealRepository;

    /**
     * 거래 확정 기한 만료 체크 스케줄러
     * 1분마다 실행되어 confirmByAt이 지난 PENDING_CONFIRMATION 상태의 거래를 EXPIRED로 변경
     */
    @Scheduled(fixedDelay = 60000) // 1분마다 실행
    @Transactional
    public void expireDeals() {
        OffsetDateTime now = OffsetDateTime.now();
        
        // PENDING_CONFIRMATION 상태이면서 confirmByAt이 현재 시간보다 이전인 거래 조회
        List<Deal> expiredDeals = dealRepository.findByStatusAndConfirmByAtBefore(
                DealStatus.PENDING_CONFIRMATION, 
                now
        );

        if (expiredDeals.isEmpty()) {
            return;
        }

        log.info("확정 기한 만료 거래 처리 시작. 대상 건수: {}", expiredDeals.size());

        for (Deal deal : expiredDeals) {
            try {
                deal.expire();
                log.info("거래 ID: {} 만료 처리 완료", deal.getDealId());
            } catch (Exception e) {
                log.error("거래 ID: {} 만료 처리 중 오류 발생", deal.getDealId(), e);
            }
        }
        
        log.info("확정 기한 만료 거래 처리 완료");
    }

    /**
     * 자동 거래 완료(COMPLETE) 처리
     *
     * 조건:
     * 1. CONFIRMED 상태
     * 2. 배송 시작 후 7일 경과 (shippedAt + 7일)
     * 3. 진행 중인 신고/분쟁이 없음
     *
     * 매 시간 정각에 실행
     */
    @Scheduled(cron = "0 0 * * * *")  // 매 시간 정각 (0분 0초)
    @Transactional
    public void autoCompleteDeal() {
        log.info("자동 거래 완료 스케줄러 시작");

        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime autoCompleteThreshold = now.minusDays(7);  // 7일 전

        // 자동 완료 가능한 거래 조회
        List<Deal> dealsToComplete = dealRepository.findCompletableDeals(autoCompleteThreshold);

        int completedCount = 0;
        for (Deal deal : dealsToComplete) {
            try {
                // 거래 완료 처리
                deal.complete();
                completedCount++;
                log.info("자동 거래 완료 처리: dealId={}, shippedAt={}",
                        deal.getDealId(), deal.getShippedAt());
            } catch (Exception e) {
                log.error("자동 거래 완료 처리 실패: dealId={}, error={}",
                        deal.getDealId(), e.getMessage());
            }
        }

        log.info("자동 거래 완료 스케줄러 종료. 처리된 거래 수: {}", completedCount);
    }
}
