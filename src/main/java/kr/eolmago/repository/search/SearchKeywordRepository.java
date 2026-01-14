package kr.eolmago.repository.search;

import kr.eolmago.domain.entity.search.SearchKeyword;
import kr.eolmago.domain.entity.search.enums.KeywordType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 검색 키워드 Repository
 */
public interface SearchKeywordRepository extends JpaRepository<SearchKeyword, Long>, SearchKeywordRepositoryCustom {

    /**
     * 키워드로 조회
     *
     * @param keyword 검색어
     * @return Optional<SearchKeyword>
     */
    Optional<SearchKeyword> findByKeyword(String keyword);

    /**
     * 브랜드 키워드만 조회
     *
     * @param keywordType 키워드 타입
     * @return 브랜드 키워드 목록
     */
    List<SearchKeyword> findByKeywordTypeOrderBySearchCountDesc(KeywordType keywordType);
}
