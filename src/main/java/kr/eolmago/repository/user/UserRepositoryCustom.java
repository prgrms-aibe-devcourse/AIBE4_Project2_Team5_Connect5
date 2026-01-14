package kr.eolmago.repository.user;

import kr.eolmago.domain.entity.user.User;
import kr.eolmago.domain.entity.user.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserRepositoryCustom {
    Page<User> findUsersWithFilters(String name, String email, UserStatus status, Pageable pageable);
}
