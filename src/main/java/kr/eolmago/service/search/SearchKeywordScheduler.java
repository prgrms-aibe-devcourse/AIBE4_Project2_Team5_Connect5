package kr.eolmago.service.search;

import kr.eolmago.domain.entity.search.SearchKeyword;
import kr.eolmago.repository.search.SearchKeywordRepository;
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
public class SearchKeywordScheduler {

    private final SearchKeywordRepository searchKeywordRepository;

    /**
     * 오래된 검색 키워드 정리 스케줄러
     *  - (사용자량이 적은 시간) 매일 새벽 3시에 실행되어
     *  - 3개월 이상 사용되지 않고 검색 횟수가 5회 미만인 키워드 자동 삭제
     *
     * 삭제 조건:
     * - lastSearchedAt이 3개월 이전
     * - searchCount가 5회 미만
     */
    @Scheduled(cron = "0 0 3 * * *")  // 매일 새벽 3시 (0분 0초)
    @Transactional
    public void cleanupInactiveKeywords() {
        log.info("오래된 검색 키워드 정리 스케줄러 시작");

        OffsetDateTime threshold = OffsetDateTime.now().minusMonths(3);  // 3개월 전
        int minCount = 5;  // 5회 미만

        // 비활성 키워드 조회
        List<SearchKeyword> inactiveKeywords = searchKeywordRepository.findInactiveKeywords(threshold, minCount);

        if (inactiveKeywords.isEmpty()) {
            log.info("삭제할 검색 키워드가 없습니다.");
            return;
        }

        log.info("삭제 대상 검색 키워드 건수: {}", inactiveKeywords.size());

        int deletedCount = 0;
        for (SearchKeyword keyword : inactiveKeywords) {
            try {
                searchKeywordRepository.delete(keyword);
                deletedCount++;
                log.debug("검색 키워드 삭제: keyword={}, searchCount={}, lastSearchedAt={}",
                        keyword.getKeyword(), keyword.getSearchCount(), keyword.getLastSearchedAt());
            } catch (Exception e) {
                log.error("검색 키워드 삭제 실패: keyword={}, error={}",
                        keyword.getKeyword(), e.getMessage());
            }
        }

        log.info("오래된 검색 키워드 정리 완료. 삭제된 키워드 수: {}", deletedCount);
    }
}