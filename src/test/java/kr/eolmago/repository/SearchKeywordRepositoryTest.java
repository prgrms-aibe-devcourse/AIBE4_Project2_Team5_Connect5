package kr.eolmago.repository;

import kr.eolmago.domain.entity.search.SearchKeyword;
import kr.eolmago.repository.search.SearchKeywordRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SearchKeywordRepository 통합 테스트
 *
 * 목적:
 * - DB Generated Column (keyword_chosung) 정상 작동 확인
 * - 초성 검색 기능 검증
 *
 * 주의:
 * - 초성 함수는 PostgreSQL 전용이므로 실제 DB 연결 필요
 * - @SpringBootTest로 전체 컨텍스트 로드 (QueryDSL 포함)
 */
@SpringBootTest
@Transactional
class SearchKeywordRepositoryTest {

    @Autowired
    private SearchKeywordRepository searchKeywordRepository;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 초기화
        searchKeywordRepository.deleteAll();
        searchKeywordRepository.flush();
    }

    @AfterEach
    void tearDown() {
        // 테스트 데이터 정리
        searchKeywordRepository.deleteAll();
        searchKeywordRepository.flush();
    }

    @Test
    @DisplayName("초성 검색: 'ㅇㅇㅍ' 검색 시 '아이폰' 키워드를 찾는다")
    void findByChosungPrefix_Success() {
        // given
        SearchKeyword keyword1 = SearchKeyword.create("아이폰");  // ㅇㅇㅍ
        SearchKeyword keyword2 = SearchKeyword.create("갤럭시");  // ㄱㄹㅅ
        SearchKeyword keyword3 = SearchKeyword.create("아이패드"); // ㅇㅇㅍㄷ

        searchKeywordRepository.save(keyword1);
        searchKeywordRepository.save(keyword2);
        searchKeywordRepository.save(keyword3);

        // when
        List<SearchKeyword> results = searchKeywordRepository.findByChosungPrefix("ㅇㅇㅍ", 10);

        // then
        assertThat(results).hasSize(2);  // 아이폰, 아이패드
        assertThat(results).extracting(SearchKeyword::getKeyword)
                .contains("아이폰");
    }

    @Test
    @DisplayName("초성 검색: 'ㄱㄹ' 검색 시 '갤럭시', '구글' 키워드를 찾는다")
    void findByChosungPrefix_Galaxy() {
        // given
        SearchKeyword keyword1 = SearchKeyword.create("아이폰");  // ㅇㅇㅍ
        SearchKeyword keyword2 = SearchKeyword.create("갤럭시");  // ㄱㄹㅅ
        SearchKeyword keyword3 = SearchKeyword.create("구글");   // ㄱㄱ

        searchKeywordRepository.save(keyword1);
        searchKeywordRepository.save(keyword2);
        searchKeywordRepository.save(keyword3);

        // when
        List<SearchKeyword> results = searchKeywordRepository.findByChosungPrefix("ㄱㄹ", 10);

        // then
        assertThat(results).hasSize(1);  // 갤럭시만 (ㄱㄹㅅ는 ㄱㄹ로 시작)
        assertThat(results.get(0).getKeyword()).isEqualTo("갤럭시");
    }

    @Test
    @DisplayName("초성 검색: 검색 횟수 내림차순 정렬")
    void findByChosungPrefix_OrderBySearchCount() {
        // given
        SearchKeyword keyword1 = SearchKeyword.create("아이폰");  // ㅇㅇㅍ
        keyword1.incrementSearchCount();
        keyword1.incrementSearchCount();  // searchCount = 3

        SearchKeyword keyword2 = SearchKeyword.create("아이패드");  // ㅇㅇㅍㄷ, searchCount = 1

        searchKeywordRepository.save(keyword1);
        searchKeywordRepository.save(keyword2);

        // when
        List<SearchKeyword> results = searchKeywordRepository.findByChosungPrefix("ㅇㅇㅍ", 10);

        // then
        assertThat(results).hasSize(2);  // 아이폰(3), 아이패드(1)
        assertThat(results.get(0).getKeyword()).isEqualTo("아이폰");  // searchCount 높은 순
        assertThat(results.get(0).getSearchCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("초성 검색: 일치하는 결과가 없으면 빈 리스트 반환")
    void findByChosungPrefix_NoResults() {
        // given
        SearchKeyword keyword = SearchKeyword.create("아이폰");  // ㅇㅇㅍ
        searchKeywordRepository.save(keyword);

        // when
        List<SearchKeyword> results = searchKeywordRepository.findByChosungPrefix("ㅋㅋ", 10);

        // then
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("초성 검색: limit 제한 정상 작동")
    void findByChosungPrefix_Limit() {
        // given
        for (int i = 1; i <= 5; i++) {
            SearchKeyword keyword = SearchKeyword.create("아이폰" + i);  // ㅇㅇㅍ1, ㅇㅇㅍ2, ...
            searchKeywordRepository.save(keyword);
        }

        // when
        List<SearchKeyword> results = searchKeywordRepository.findByChosungPrefix("ㅇㅇㅍ", 3);

        // then
        assertThat(results).hasSize(3);
    }

    @Test
    @DisplayName("일반 prefix 검색: '아이' 검색 시 '아이폰' 키워드를 찾는다")
    void findByKeywordPrefix_Success() {
        // given
        SearchKeyword keyword1 = SearchKeyword.create("아이폰");
        SearchKeyword keyword2 = SearchKeyword.create("갤럭시");
        SearchKeyword keyword3 = SearchKeyword.create("아이패드");

        searchKeywordRepository.save(keyword1);
        searchKeywordRepository.save(keyword2);
        searchKeywordRepository.save(keyword3);

        // when
        List<SearchKeyword> results = searchKeywordRepository.findByKeywordPrefix("아이", 10);

        // then
        assertThat(results).hasSize(2);
        assertThat(results).extracting(SearchKeyword::getKeyword)
                .containsExactlyInAnyOrder("아이폰", "아이패드");
    }
}
