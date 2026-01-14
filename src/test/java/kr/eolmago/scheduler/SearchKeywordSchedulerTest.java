package kr.eolmago.scheduler;

import kr.eolmago.domain.entity.search.SearchKeyword;
import kr.eolmago.repository.search.SearchKeywordRepository;
import kr.eolmago.service.search.SearchKeywordScheduler;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchKeywordSchedulerTest {

    @InjectMocks
    private SearchKeywordScheduler searchKeywordScheduler;

    @Mock
    private SearchKeywordRepository searchKeywordRepository;

    @Mock
    private SearchKeyword keyword;

    @Test
    @DisplayName("비활성 검색 키워드가 있으면 삭제 처리된다")
    void cleanupInactiveKeywords_Success() {
        // given
        given(searchKeywordRepository.findInactiveKeywords(any(OffsetDateTime.class), anyInt()))
                .willReturn(List.of(keyword));

        given(keyword.getKeyword()).willReturn("test");
        given(keyword.getSearchCount()).willReturn(3);
        given(keyword.getLastSearchedAt()).willReturn(OffsetDateTime.now().minusMonths(4));

        // when
        searchKeywordScheduler.cleanupInactiveKeywords();

        // then
        verify(searchKeywordRepository, times(1)).delete(keyword);
    }

    @Test
    @DisplayName("비활성 검색 키워드가 없으면 아무 작업도 하지 않는다")
    void cleanupInactiveKeywords_NoKeywords() {
        // given
        given(searchKeywordRepository.findInactiveKeywords(any(OffsetDateTime.class), anyInt()))
                .willReturn(Collections.emptyList());

        // when
        searchKeywordScheduler.cleanupInactiveKeywords();

        // then
        verify(searchKeywordRepository, never()).delete(any(SearchKeyword.class));
    }

    @Test
    @DisplayName("키워드 삭제 중 예외가 발생해도 다른 키워드 처리에 영향을 주지 않는다")
    void cleanupInactiveKeywords_ExceptionHandling() {
        // given
        SearchKeyword keyword1 = mock(SearchKeyword.class);
        SearchKeyword keyword2 = mock(SearchKeyword.class);

        given(searchKeywordRepository.findInactiveKeywords(any(OffsetDateTime.class), anyInt()))
                .willReturn(List.of(keyword1, keyword2));

        given(keyword1.getKeyword()).willReturn("test1");
        given(keyword2.getKeyword()).willReturn("test2");

        // keyword1 삭제 시 예외 발생
        doThrow(new RuntimeException("Error")).when(searchKeywordRepository).delete(keyword1);

        // when
        searchKeywordScheduler.cleanupInactiveKeywords();

        // then
        verify(searchKeywordRepository, times(1)).delete(keyword1);
        verify(searchKeywordRepository, times(1)).delete(keyword2); // keyword1 실패와 무관하게 keyword2는 실행되어야 함
    }
}