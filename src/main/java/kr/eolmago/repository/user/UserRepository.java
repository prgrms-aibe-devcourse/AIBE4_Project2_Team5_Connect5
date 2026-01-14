package kr.eolmago.repository.user;

import kr.eolmago.domain.entity.user.User;
import kr.eolmago.domain.entity.user.enums.UserRole;
import kr.eolmago.domain.entity.user.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID>, UserRepositoryCustom {

    Optional<User> findByUserId(UUID userId);

    List<User> findByRole(UserRole role);

    List<User> findByStatus(UserStatus status);
}
