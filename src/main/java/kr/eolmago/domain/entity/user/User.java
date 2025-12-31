package kr.eolmago.domain.entity.user;

import jakarta.persistence.*;
import kr.eolmago.domain.entity.common.AuditableEntity;
import kr.eolmago.domain.entity.user.enums.UserRole;
import kr.eolmago.domain.entity.user.enums.UserStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    public static User create(
            UserRole role
    ) {
        User user = new User();
        user.role = role;
        user.status = UserStatus.ACTIVE;
        return user;
    }

    public void updateRole(UserRole newRole) {
        this.role = newRole;
    }

    public void updateStatus(UserStatus newStatus) {
        this.status = newStatus;
    }
}