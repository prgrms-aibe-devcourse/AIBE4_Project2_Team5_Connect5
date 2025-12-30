package kr.eolmago.domain.entity.user;

import kr.eolmago.domain.entity.common.CreatedAtEntity;
import kr.eolmago.domain.entity.report.Report;
import kr.eolmago.domain.entity.user.enums.PenaltyType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "user_penalties")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPenalty extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, updatable = false)
    private Long penaltyId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) // 필수 연관관계
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false, unique = true)
    private Report report;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PenaltyType type;

    @Column(nullable = false, length = 50)
    private String reason;

    @Column(nullable = false)
    private OffsetDateTime startedAt;

    @Column
    private OffsetDateTime expiresAt; // null이면 무기한

    public static UserPenalty create(
            User user,
            Report report,
            PenaltyType type,
            String reason,
            OffsetDateTime startedAt,
            OffsetDateTime expiresAt
    ) {
        UserPenalty penalty = new UserPenalty();
        penalty.user = user;
        penalty.report = report;
        penalty.type = type;
        penalty.reason = reason;
        penalty.startedAt = startedAt;
        penalty.expiresAt = expiresAt;
        return penalty;
    }
}