package kr.eolmago.scheduler;

import kr.eolmago.domain.entity.deal.Deal;
import kr.eolmago.domain.entity.deal.enums.DealStatus;
import kr.eolmago.repository.deal.DealRepository;
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
    @DisplayName("만료된 거래가 있으면 자동으로 종료 처리된다")
    void terminateExpiredDeals_Success() {
        // given
        given(dealRepository.findByStatusAndConfirmByAtBefore(eq(DealStatus.PENDING_CONFIRMATION), any(OffsetDateTime.class)))
                .willReturn(List.of(deal));
        
        given(deal.getDealId()).willReturn(1L);

        // when
        dealScheduler.terminateExpiredDeals();

        // then
        verify(deal, times(1)).terminate(anyString());
    }

    @Test
    @DisplayName("만료된 거래가 없으면 아무 작업도 하지 않는다")
    void terminateExpiredDeals_NoDeals() {
        // given
        given(dealRepository.findByStatusAndConfirmByAtBefore(eq(DealStatus.PENDING_CONFIRMATION), any(OffsetDateTime.class)))
                .willReturn(Collections.emptyList());

        // when
        dealScheduler.terminateExpiredDeals();

        // then
        verify(deal, never()).terminate(anyString());
    }
    
    @Test
    @DisplayName("거래 종료 처리 중 예외가 발생해도 다른 거래 처리에 영향을 주지 않는다")
    void terminateExpiredDeals_ExceptionHandling() {
        // given
        Deal deal1 = mock(Deal.class);
        Deal deal2 = mock(Deal.class);
        
        given(dealRepository.findByStatusAndConfirmByAtBefore(eq(DealStatus.PENDING_CONFIRMATION), any(OffsetDateTime.class)))
                .willReturn(List.of(deal1, deal2));
        
        given(deal1.getDealId()).willReturn(1L);
        given(deal2.getDealId()).willReturn(2L);
        
        // deal1 처리 시 예외 발생
        doThrow(new RuntimeException("Error")).when(deal1).terminate(anyString());

        // when
        dealScheduler.terminateExpiredDeals();

        // then
        verify(deal1, times(1)).terminate(anyString());
        verify(deal2, times(1)).terminate(anyString()); // deal1 실패와 무관하게 deal2는 실행되어야 함
    }
}
