package kr.eolmago.global.config;

import kr.eolmago.domain.entity.auction.Auction;
import kr.eolmago.domain.entity.auction.AuctionItem;
import kr.eolmago.domain.entity.auction.enums.AuctionStatus;
import kr.eolmago.domain.entity.auction.enums.ItemCategory;
import kr.eolmago.domain.entity.auction.enums.ItemCondition;
import kr.eolmago.domain.entity.user.User;
import kr.eolmago.domain.entity.user.enums.UserRole;
import kr.eolmago.repository.auction.AuctionItemRepository;
import kr.eolmago.repository.auction.AuctionRepository;
import kr.eolmago.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 더미 데이터 생성 클래스
 * 애플리케이션 시작 시 테스트용 데이터를 생성합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final AuctionItemRepository auctionItemRepository;
    private final AuctionRepository auctionRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("========== 더미 데이터 생성 시작 ==========");

        // 1. 사용자 생성
        List<User> users = createUsers();
        log.info("사용자 {} 명 생성 완료", users.size());

        // 2. 경매 데이터 생성 (페이지네이션 테스트를 위해 50개 생성)
        List<Auction> auctions = createAuctions(users);
        log.info("경매 {} 개 생성 완료", auctions.size());

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
                10000,
                durationHours,
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
                20000,
                durationHours,
                startAt,
                endAt
            );

            auctions.add(auctionRepository.save(auction));
        }

        return auctions;
    }
}