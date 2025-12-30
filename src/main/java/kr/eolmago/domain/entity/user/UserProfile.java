package kr.eolmago.domain.entity.user;

import kr.eolmago.domain.entity.common.AuditableEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "user_profile")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserProfile extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, updatable = false)
    private Long profileId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 30)
    private String name;

    @Column(nullable = false, length = 30)
    private String nickname;

    @Column(length = 20)
    private String phoneNumber;

    @Column(nullable = false)
    private boolean phoneVerified;

    @Column(columnDefinition = "TEXT")
    private String profileImageUrl;

    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal ratingAvg;

    @Column(nullable = false)
    private int tradeCount;

    @Column(nullable = false)
    private short reportCount;

    public static UserProfile create( // 번호 인증 O
            User user,
            String name,
            String nickname,
            String phoneNumber,
            boolean phoneVerified
    ) {
        UserProfile profile = new UserProfile();
        profile.user = user;
        profile.name = name;
        profile.nickname = nickname;
        profile.phoneNumber = phoneNumber;
        profile.phoneVerified = phoneVerified;
        profile.ratingAvg = new BigDecimal("0.00");
        profile.tradeCount = 0;
        profile.reportCount = 0;

        return profile;
    }

    public static UserProfile create( // 번호 인증 X
            User user,
            String name,
            String nickname
    ) {
        return create(user, name, nickname, null, false);
    }
}