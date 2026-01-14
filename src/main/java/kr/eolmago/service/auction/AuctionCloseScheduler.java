package kr.eolmago.service.auction;

import kr.eolmago.domain.entity.auction.enums.AuctionStatus;
import kr.eolmago.dto.view.auction.AuctionEndAtView;
import kr.eolmago.repository.auction.AuctionCloseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicLong;

import static kr.eolmago.service.auction.constants.AuctionConstants.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionCloseScheduler {

    private final TaskScheduler taskScheduler;
    private final AuctionCloseService auctionCloseService;
    private final AuctionCloseRepository auctionCloseRepository;

    private final AtomicLong tokenSeq = new AtomicLong(0);
    private final ConcurrentMap<UUID, ScheduledRef> scheduled = new ConcurrentHashMap<>();

    public void scheduleOrReschedule(UUID auctionId, OffsetDateTime endAt) {
        if (auctionId == null || endAt == null) return;

        long token = tokenSeq.incrementAndGet();

        Instant runAt = endAt.toInstant();
        Instant now = Instant.now();
        if (runAt.isBefore(now)) {
            runAt = now;
        }

        ScheduledRef newRef = new ScheduledRef(token);

        ScheduledRef prev = scheduled.put(auctionId, newRef);
        if (prev != null && prev.future != null) {
            prev.future.cancel(false);
        }

        try {
            ScheduledFuture<?> future = taskScheduler.schedule(() -> runCloseSafely(auctionId, token), runAt);

            if (future == null) {
                scheduled.computeIfPresent(auctionId, (id, ref) -> ref.token == token ? null : ref);
                log.warn("[AUC_CLOSE_SCH_NULL] 경매 마감 스케줄 등록 실패(null). auctionId={}, runAt={}", auctionId, runAt);
                return;
            }

            newRef.future = future;

        } catch (TaskRejectedException e) {
            scheduled.computeIfPresent(auctionId, (id, ref) -> ref.token == token ? null : ref);
            log.warn("[AUC_CLOSE_SCH_REJECTED] 경매 마감 스케줄 등록 거절. auctionId={}, runAt={}, cause={}",
                    auctionId, runAt, e.toString());
        } catch (RuntimeException e) {
            scheduled.computeIfPresent(auctionId, (id, ref) -> ref.token == token ? null : ref);
            log.error("[AUC_CLOSE_SCH_ERROR] 경매 마감 스케줄 등록 오류. auctionId={}, runAt={}, cause={}",
                    auctionId, runAt, e.toString());
        }
    }

    private void runCloseSafely(UUID auctionId, long token) {
        try {
            auctionCloseService.closeAuction(auctionId);
        } catch (Exception e) {
            log.error("경매 마감 실행 실패. auctionId={}, token={}", auctionId, token, e);
        } finally {
            scheduled.computeIfPresent(auctionId, (id, ref) -> ref.token == token ? null : ref);
        }
    }

    // 서버 재시작 시 LIVE 경매들의 endAt을 다시 스케줄에 등록
    @EventListener(ApplicationReadyEvent.class)
    public void bootstrapOnStartup() {
        try {
            List<AuctionEndAtView> lives = auctionCloseRepository.findAllEndAtByStatus(AuctionStatus.LIVE);

            int count = 0;
            for (AuctionEndAtView v : lives) {
                if (v.auctionId() == null || v.endAt() == null) continue;
                scheduleOrReschedule(v.auctionId(), v.endAt());
                count++;
            }
            log.info("경매 마감 부트스트랩 완료. scheduledCount={}", count);
        } catch (Exception e) {
            log.error("경매 마감 부트스트랩 실패.", e);
        }
    }

    // endAt <= now 인 LIVE를 주기적으로 마감
    @Scheduled(fixedDelay = 60000)
    public void sweepOverdueAuctions() {
        try {
            OffsetDateTime now = OffsetDateTime.now();

            List<UUID> ids = auctionCloseRepository.findIdsToClose(
                    AuctionStatus.LIVE,
                    now,
                    PageRequest.of(0, SWEEP_PAGE_SIZE)
            );

            if (ids.isEmpty()) return;

            log.info("경매 마감 스위프 시작. count={}", ids.size());

            for (UUID id : ids) {
                try {
                    auctionCloseService.closeAuction(id);
                } catch (Exception e) {
                    log.error("경매 마감 스위프 개별 실패. auctionId={}", id, e);
                }
            }
        } catch (Exception e) {
            log.error("경매 마감 스위프 실행 중 오류.", e);
        }
    }

    private static final class ScheduledRef {
        private final long token;
        private volatile ScheduledFuture<?> future;

        private ScheduledRef(long token) {
            this.token = token;
        }
    }
}