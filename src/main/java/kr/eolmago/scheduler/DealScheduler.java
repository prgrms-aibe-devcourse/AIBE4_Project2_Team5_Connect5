package kr.eolmago.scheduler;

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
     * 1분마다 실행되어 confirmByAt이 지난 PENDING_CONFIRMATION 상태의 거래를 TERMINATED로 변경
     */
    @Scheduled(fixedDelay = 60000) // 1분마다 실행
    @Transactional
    public void terminateExpiredDeals() {
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
                deal.terminate("거래 확정 기한 만료로 인한 자동 종료");
                log.info("거래 ID: {} 자동 종료 처리 완료", deal.getDealId());
            } catch (Exception e) {
                log.error("거래 ID: {} 자동 종료 처리 중 오류 발생", deal.getDealId(), e);
            }
        }
        
        log.info("확정 기한 만료 거래 처리 완료");
    }
}
