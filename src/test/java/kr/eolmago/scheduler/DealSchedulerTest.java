package kr.eolmago.scheduler;

import kr.eolmago.domain.entity.deal.Deal;
import kr.eolmago.domain.entity.deal.enums.DealStatus;
import kr.eolmago.repository.deal.DealRepository;
import kr.eolmago.service.deal.DealScheduler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DealSchedulerTest {

    @InjectMocks
    private DealScheduler dealScheduler;

    @Mock
    private DealRepository dealRepository;

    @Mock
    private Deal deal;

    @Test
    @DisplayName("만료된 거래가 있으면 자동으로 만료 처리된다")
    void expireDeals_Success() {
        // given
        given(dealRepository.findByStatusAndConfirmByAtBefore(eq(DealStatus.PENDING_CONFIRMATION), any(OffsetDateTime.class)))
                .willReturn(List.of(deal));
        
        given(deal.getDealId()).willReturn(1L);

        // when
        dealScheduler.expireDeals();

        // then
        verify(deal, times(1)).expire();
    }

    @Test
    @DisplayName("만료된 거래가 없으면 아무 작업도 하지 않는다")
    void expireDeals_NoDeals() {
        // given
        given(dealRepository.findByStatusAndConfirmByAtBefore(eq(DealStatus.PENDING_CONFIRMATION), any(OffsetDateTime.class)))
                .willReturn(Collections.emptyList());

        // when
        dealScheduler.expireDeals();

        // then
        verify(deal, never()).expire();
    }
    
    @Test
    @DisplayName("거래 만료 처리 중 예외가 발생해도 다른 거래 처리에 영향을 주지 않는다")
    void expireDeals_ExceptionHandling() {
        // given
        Deal deal1 = mock(Deal.class);
        Deal deal2 = mock(Deal.class);

        given(dealRepository.findByStatusAndConfirmByAtBefore(eq(DealStatus.PENDING_CONFIRMATION), any(OffsetDateTime.class)))
                .willReturn(List.of(deal1, deal2));

        given(deal1.getDealId()).willReturn(1L);
        given(deal2.getDealId()).willReturn(2L);

        // deal1 처리 시 예외 발생
        doThrow(new RuntimeException("Error")).when(deal1).expire();

        // when
        dealScheduler.expireDeals();

        // then
        verify(deal1, times(1)).expire();
        verify(deal2, times(1)).expire(); // deal1 실패와 무관하게 deal2는 실행되어야 함
    }

    @Test
    @DisplayName("배송 시작 후 7일이 지나고 신고가 없는 거래는 자동으로 완료 처리된다")
    void autoCompleteDeal_Success() {
        // given
        given(dealRepository.findCompletableDeals(any(OffsetDateTime.class)))
                .willReturn(List.of(deal));

        given(deal.getDealId()).willReturn(1L);
        given(deal.getShippedAt()).willReturn(OffsetDateTime.now().minusDays(7));

        // when
        dealScheduler.autoCompleteDeal();

        // then
        verify(deal, times(1)).complete();
    }

    @Test
    @DisplayName("자동 완료 대상 거래가 없으면 아무 작업도 하지 않는다")
    void autoCompleteDeal_NoDeals() {
        // given
        given(dealRepository.findCompletableDeals(any(OffsetDateTime.class)))
                .willReturn(Collections.emptyList());

        // when
        dealScheduler.autoCompleteDeal();

        // then
        verify(deal, never()).complete();
    }

    @Test
    @DisplayName("거래 완료 처리 중 예외가 발생해도 다른 거래 처리에 영향을 주지 않는다")
    void autoCompleteDeal_ExceptionHandling() {
        // given
        Deal deal1 = mock(Deal.class);
        Deal deal2 = mock(Deal.class);

        given(dealRepository.findCompletableDeals(any(OffsetDateTime.class)))
                .willReturn(List.of(deal1, deal2));

        given(deal1.getDealId()).willReturn(1L);
        given(deal2.getDealId()).willReturn(2L);
        given(deal2.getShippedAt()).willReturn(OffsetDateTime.now().minusDays(8));

        // deal1 처리 시 예외 발생
        doThrow(new RuntimeException("Error")).when(deal1).complete();

        // when
        dealScheduler.autoCompleteDeal();

        // then
        verify(deal1, times(1)).complete();
        verify(deal2, times(1)).complete(); // deal1 실패와 무관하게 deal2는 실행되어야 함
    }
}
