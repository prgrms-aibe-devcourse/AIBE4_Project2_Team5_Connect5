package kr.eolmago.global.util;

import kr.eolmago.domain.entity.auction.Auction;
import kr.eolmago.domain.entity.auction.AuctionItem;
import kr.eolmago.domain.entity.auction.enums.AuctionStatus;
import kr.eolmago.domain.entity.auction.enums.ItemCategory;
import kr.eolmago.domain.entity.auction.enums.ItemCondition;
import kr.eolmago.domain.entity.search.SearchKeyword;
import kr.eolmago.domain.entity.user.User;
import kr.eolmago.domain.entity.user.enums.UserRole;
import kr.eolmago.repository.auction.AuctionItemRepository;
import kr.eolmago.repository.auction.AuctionRepository;
import kr.eolmago.repository.search.SearchKeywordRepository;
import kr.eolmago.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 더미 데이터 생성 클래스
 * 애플리케이션 시작 시 테스트용 데이터를 생성합니다.
 *   - User, Auction, AuctionItem, SearchKeyword 생성
 *  - 개발 환경에서만 사용 (프로덕션에선 비활성화)
 */
@Slf4j
//@Component
@RequiredArgsConstructor
public class DataInitializer /*implements ApplicationRunner*/ {

    private final UserRepository userRepository;
    private final AuctionItemRepository auctionItemRepository;
    private final AuctionRepository auctionRepository;
    private final SearchKeywordRepository searchKeywordRepository;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String AUTOCOMPLETE_KEY = "autocomplete:all";

//    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("========== 더미 데이터 생성 시작 ==========");

        // 1. 사용자 생성
        List<User> users = createUsers();
        log.info("사용자 {} 명 생성 완료", users.size());

        // 2. 경매 데이터 생성 (페이지네이션 테스트를 위해 50개 생성)
        List<Auction> auctions = createAuctions(users);
        log.info("경매 {} 개 생성 완료", auctions.size());

        // 3. 검색 키워드 생성 (DB + Redis)
        List<SearchKeyword> keywords = createSearchKeywords();
        log.info("검색 키워드 {} 개 생성 완료 (DB + Redis)", keywords.size());

        log.info("========== 더미 데이터 생성 완료 ==========");
    }

    private List<User> createUsers() {
        List<User> users = new ArrayList<>();

        // 판매자 10명 생성
        for (int i = 1; i <= 10; i++) {
            User user = User.create(UserRole.USER);
            users.add(userRepository.save(user));
        }

        return users;
    }

    private List<Auction> createAuctions(List<User> users) {
        List<Auction> auctions = new ArrayList<>();

        // 휴대폰 더미 데이터
        String[] phoneNames = {
                "아이폰 15 Pro Max", "갤럭시 S24 Ultra", "아이폰 14 Pro", "갤럭시 Z Flip5",
                "아이폰 13", "갤럭시 S23", "아이폰 15", "갤럭시 A54",
                "아이폰 14", "갤럭시 S23 FE", "아이폰 SE 3세대", "갤럭시 Z Fold5"
        };

        String[] phoneBrands = {
                "Apple", "Samsung", "Apple", "Samsung",
                "Apple", "Samsung", "Apple", "Samsung",
                "Apple", "Samsung", "Apple", "Samsung"
        };

        // 태블릿 더미 데이터
        String[] tabletNames = {
                "아이패드 Pro 12.9", "갤럭시 탭 S9 Ultra", "아이패드 Air 5세대", "갤럭시 탭 S9",
                "아이패드 10세대", "갤럭시 탭 A9+", "아이패드 mini 6", "갤럭시 탭 S8"
        };

        String[] tabletBrands = {
                "Apple", "Samsung", "Apple", "Samsung",
                "Apple", "Samsung", "Apple", "Samsung"
        };

        OffsetDateTime now = OffsetDateTime.now();

        // 휴대폰 30개 생성
        for (int i = 0; i < 30; i++) {
            int phoneIndex = i % phoneNames.length;
            User seller = users.get(i % users.size());

            // AuctionItem 생성
            Map<String, Object> specs = new HashMap<>();
            specs.put("brand", phoneBrands[phoneIndex]);
            specs.put("model", phoneNames[phoneIndex]);
            specs.put("network", "5G");
            specs.put("releaseYear", 2023 - (i % 3));
            specs.put("storageGb", (i % 3 + 1) * 128);
            specs.put("color", i % 2 == 0 ? "블랙" : "화이트");

            AuctionItem item = AuctionItem.create(
                    phoneNames[phoneIndex],
                    ItemCategory.PHONE,
                    ItemCondition.values()[i % 4], // S, A, B, C 순환
                    specs
            );
            auctionItemRepository.save(item);

            // Auction 생성
            int basePrice = 300000 + (i * 50000);
            int durationHours = 24 + (i % 3) * 24; // 24, 48, 72시간
            int bidIncrement = 5000;               // 원하는 값(최소단위)

            // 상태 분산: LIVE(60%), ENDED_SOLD(20%), ENDED_UNSOLD(10%), DRAFT(10%)
            AuctionStatus status;
            OffsetDateTime startAt;
            OffsetDateTime endAt;

            if (i < 18) { // 60% LIVE
                status = AuctionStatus.LIVE;
                startAt = now.minusHours(i % 12);
                endAt = now.plusHours(durationHours - (i % 12));
            } else if (i < 24) { // 20% ENDED_SOLD
                status = AuctionStatus.ENDED_SOLD;
                startAt = now.minusDays(5 + (i % 3));
                endAt = now.minusDays(2 + (i % 2));
            } else if (i < 27) { // 10% ENDED_UNSOLD
                status = AuctionStatus.ENDED_UNSOLD;
                startAt = now.minusDays(4);
                endAt = now.minusDays(1);
            } else { // 10% DRAFT
                status = AuctionStatus.DRAFT;
                startAt = null;
                endAt = null;
            }

            Auction auction = Auction.create(
                    item,
                    seller,
                    phoneNames[phoneIndex] + " - " + specs.get("storageGb") + "GB " + specs.get("color"),
                    String.format("%s %s 판매합니다. 상태: %s급, 용량: %dGB",
                            specs.get("brand"), specs.get("model"),
                            ItemCondition.values()[i % 4].name(),
                            specs.get("storageGb")),
                    status,
                    basePrice,
                    durationHours,
                    bidIncrement,
                    startAt,
                    endAt
            );

            auctions.add(auctionRepository.save(auction));
        }

        // 태블릿 20개 생성
        for (int i = 0; i < 20; i++) {
            int tabletIndex = i % tabletNames.length;
            User seller = users.get(i % users.size());

            // AuctionItem 생성
            Map<String, Object> specs = new HashMap<>();
            specs.put("brand", tabletBrands[tabletIndex]);
            specs.put("model", tabletNames[tabletIndex]);
            specs.put("screenSize", i % 2 == 0 ? "11인치" : "12.9인치");
            specs.put("releaseYear", 2023 - (i % 2));
            specs.put("storageGb", (i % 3 + 2) * 128);
            specs.put("cellular", i % 3 == 0 ? "Wi-Fi + Cellular" : "Wi-Fi");

            AuctionItem item = AuctionItem.create(
                    tabletNames[tabletIndex],
                    ItemCategory.TABLET,
                    ItemCondition.values()[i % 4],
                    specs
            );
            auctionItemRepository.save(item);

            // Auction 생성
            int basePrice = 500000 + (i * 70000);
            int durationHours = 48 + (i % 2) * 24;
            int bidIncrement = 10000;

            AuctionStatus status;
            OffsetDateTime startAt;
            OffsetDateTime endAt;

            if (i < 12) { // 60% LIVE
                status = AuctionStatus.LIVE;
                startAt = now.minusHours(i % 10);
                endAt = now.plusHours(durationHours - (i % 10));
            } else if (i < 16) { // 20% ENDED_SOLD
                status = AuctionStatus.ENDED_SOLD;
                startAt = now.minusDays(4 + (i % 2));
                endAt = now.minusDays(1);
            } else if (i < 18) { // 10% ENDED_UNSOLD
                status = AuctionStatus.ENDED_UNSOLD;
                startAt = now.minusDays(3);
                endAt = now.minusHours(12);
            } else { // 10% DRAFT
                status = AuctionStatus.DRAFT;
                startAt = null;
                endAt = null;
            }

            Auction auction = Auction.create(
                    item,
                    seller,
                    tabletNames[tabletIndex] + " - " + specs.get("storageGb") + "GB",
                    String.format("%s %s 판매합니다. 화면: %s, 상태: %s급",
                            specs.get("brand"), specs.get("model"),
                            specs.get("screenSize"),
                            ItemCondition.values()[i % 4].name()),
                    status,
                    basePrice,
                    bidIncrement,
                    durationHours,
                    startAt,
                    endAt
            );

            auctions.add(auctionRepository.save(auction));
        }

        return auctions;
    }

    /**
     * 검색 키워드 더미 데이터 생성 (중복 체크 포함)
     *
     * 역할:
     * - 자동완성 테스트용 초기 데이터 생성
     * - 인기 검색어 표시용 데이터
     *
     * 연결 부분:
     * - SearchKeywordService 자동완성 API에서 사용
     * - 실제 검색 시 카운트 증가하며 갱신됨
     *
     * @return 생성된 SearchKeyword 목록
     */
    private List<SearchKeyword> createSearchKeywords() {
        List<SearchKeyword> keywords = new ArrayList<>();
        ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();

        // 검색어와 검색량 데이터
        Map<String, Integer> keywordData = new HashMap<>();

        // 브랜드 검색어
        keywordData.put("아이폰", 1247);
        keywordData.put("갤럭시", 1134);
        keywordData.put("애플", 892);
        keywordData.put("삼성", 567);

        // 모델명 검색어
        keywordData.put("아이폰 15", 856);
        keywordData.put("아이폰 14 프로", 523);
        keywordData.put("아이폰 13", 421);
        keywordData.put("갤럭시 S24", 678);
        keywordData.put("갤럭시 S23", 534);
        keywordData.put("갤럭시 Z플립5", 312);

        // 일반 검색어
        keywordData.put("중고폰", 345);
        keywordData.put("새제품", 234);
        keywordData.put("아이패드", 456);
        keywordData.put("갤럭시탭", 289);

        for (Map.Entry<String, Integer> entry : keywordData.entrySet()) {
            String keyword = entry.getKey();
            Integer searchCount = entry.getValue();

            // 중복 체크
            if (searchKeywordRepository.findByKeyword(keyword).isPresent()) {
                log.debug("검색 키워드 이미 존재, 스킵: keyword={}", keyword);
                continue;  // 이미 있으면 건너뛰기
            }

            // 1. DB 저장
            SearchKeyword searchKeyword = SearchKeyword.create(keyword);

            // searchCount 설정
            for (int i = 1; i < searchCount; i++) {
                searchKeyword.incrementSearchCount();
            }

            keywords.add(searchKeywordRepository.save(searchKeyword));

            // 2. Redis 저장
            // 점수 계산: searchCount + 브랜드 가중치
            int brandWeight = isBrandKeyword(keyword) ? 100 : 0;
            double score = searchCount + brandWeight;

            zSetOps.add(AUTOCOMPLETE_KEY, keyword, score);

            log.debug("검색 키워드 생성: keyword={}, count={}, type={}, redis_score={}",
                    keyword, searchKeyword.getSearchCount(), searchKeyword.getKeywordType(), score);
        }

        log.info("Redis 자동완성 데이터 {} 개 저장 완료", keywords.size());

        return keywords;
    }

    /**
     * 브랜드 키워드 판단 (SearchKeywordService와 동일)
     */
    private boolean isBrandKeyword(String keyword) {
        String lowerKeyword = keyword.toLowerCase();
        return lowerKeyword.matches(".*(아이폰|갤럭시|픽셀|샤오미|애플|삼성|apple|samsung|google|xiaomi).*");
    }

}