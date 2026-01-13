package kr.eolmago.scheduler;

import jakarta.persistence.EntityManager;
import kr.eolmago.domain.entity.auction.Auction;
import kr.eolmago.domain.entity.auction.AuctionItem;
import kr.eolmago.domain.entity.auction.enums.AuctionStatus;
import kr.eolmago.domain.entity.auction.enums.ItemCategory;
import kr.eolmago.domain.entity.auction.enums.ItemCondition;
import kr.eolmago.domain.entity.report.Report;
import kr.eolmago.domain.entity.report.enums.ReportReason;
import kr.eolmago.domain.entity.report.enums.ReportTargetType;
import kr.eolmago.domain.entity.user.User;
import kr.eolmago.domain.entity.user.UserPenalty;
import kr.eolmago.domain.entity.user.enums.PenaltyType;
import kr.eolmago.domain.entity.user.enums.UserRole;
import kr.eolmago.domain.entity.user.enums.UserStatus;
import kr.eolmago.repository.auction.AuctionItemRepository;
import kr.eolmago.repository.auction.AuctionRepository;
import kr.eolmago.repository.report.ReportRepository;
import kr.eolmago.repository.user.UserPenaltyRepository;
import kr.eolmago.repository.user.UserRepository;
import kr.eolmago.service.user.UserPenaltyScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class UserPenaltySchedulerTest {

    @Autowired
    private UserPenaltyScheduler userPenaltyScheduler;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserPenaltyRepository userPenaltyRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private AuctionItemRepository auctionItemRepository;

    @Autowired
    private EntityManager em;

    private User reporter;
    private User reportedUser;
    private Auction auction;
    private Report report;

    @BeforeEach
    void setUp() {
        reporter = User.create(UserRole.USER);
        userRepository.save(reporter);

        reportedUser = User.create(UserRole.USER);
        reportedUser.updateStatus(UserStatus.SUSPENDED);
        userRepository.save(reportedUser);

        AuctionItem auctionItem = AuctionItem.create("Test Item", ItemCategory.PHONE, ItemCondition.S, new HashMap<>());
        auctionItemRepository.save(auctionItem);

        OffsetDateTime now = OffsetDateTime.now();
        auction = Auction.create(auctionItem, reporter, "Test Auction", "Test Description", AuctionStatus.DRAFT, 10000, 1000, 24, now, now.plusDays(1));
        auctionRepository.save(auction);

        report = Report.create(reporter, reportedUser, auction, ReportTargetType.AUCTION, ReportReason.FRAUD_SUSPECT, "Test Content");
        reportRepository.save(report);
    }

    @Test
    @DisplayName("제재 기간이 만료된 유저의 상태를 ACTIVE로 변경한다")
    void releaseSuspendedUsers_Success() {
        // given
        OffsetDateTime now = OffsetDateTime.now();
        UserPenalty penalty = UserPenalty.create(
                reportedUser,
                report,
                PenaltyType.SUSPENDED,
                "Test Reason",
                now.minusDays(2),
                now.minusDays(1) // 만료 시간을 과거로 설정
        );
        userPenaltyRepository.save(penalty);

        em.flush();
        em.clear();

        // when
        userPenaltyScheduler.releaseSuspendedUsers();

        // then
        User foundUser = userRepository.findById(reportedUser.getUserId()).orElseThrow();
        assertThat(foundUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("제재 기간이 만료되지 않은 유저는 SUSPENDED 상태를 유지한다")
    void releaseSuspendedUsers_PenaltyNotExpired() {
        // given
        OffsetDateTime now = OffsetDateTime.now();
        UserPenalty penalty = UserPenalty.create(
                reportedUser,
                report,
                PenaltyType.SUSPENDED,
                "Test Reason",
                now.minusDays(1),
                now.plusDays(1) // 만료 시간을 미래로 설정
        );
        userPenaltyRepository.save(penalty);

        em.flush();
        em.clear();

        // when
        userPenaltyScheduler.releaseSuspendedUsers();

        // then
        User foundUser = userRepository.findById(reportedUser.getUserId()).orElseThrow();
        assertThat(foundUser.getStatus()).isEqualTo(UserStatus.SUSPENDED);
    }

    @Test
    @DisplayName("만료된 제재 외에 다른 활성 제재가 있으면 SUSPENDED 상태를 유지한다")
    void releaseSuspendedUsers_HasAnotherActivePenalty() {
        // given
        OffsetDateTime now = OffsetDateTime.now();

        // 만료된 페널티 생성
        UserPenalty expiredPenalty = UserPenalty.create(
                reportedUser,
                report,
                PenaltyType.SUSPENDED,
                "Expired Reason",
                now.minusDays(2),
                now.minusDays(1)
        );
        userPenaltyRepository.save(expiredPenalty);

        // 아직 활성 상태인 다른 페널티를 위한 새로운 리포트 생성
        Report anotherReport = Report.create(reporter, reportedUser, auction, ReportTargetType.AUCTION, ReportReason.FRAUD_SUSPECT, "Another Content");
        reportRepository.save(anotherReport);

        // 활성 페널티 생성
        UserPenalty activePenalty = UserPenalty.create(
                reportedUser,
                anotherReport,
                PenaltyType.SUSPENDED,
                "Active Reason",
                now.minusDays(1),
                now.plusDays(1)
        );
        userPenaltyRepository.save(activePenalty);

        em.flush();
        em.clear();

        // when
        userPenaltyScheduler.releaseSuspendedUsers();

        // then
        User foundUser = userRepository.findById(reportedUser.getUserId()).orElseThrow();
        assertThat(foundUser.getStatus()).isEqualTo(UserStatus.SUSPENDED);
    }
}