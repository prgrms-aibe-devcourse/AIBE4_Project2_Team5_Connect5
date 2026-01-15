package kr.eolmago.repository.user;

import kr.eolmago.domain.entity.user.User;
import kr.eolmago.domain.entity.user.UserPenalty;
import kr.eolmago.domain.entity.user.enums.PenaltyType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserPenaltyRepositoryCustom {

    List<UserPenalty> findExpiredPenalties(OffsetDateTime now);

    boolean existsActivePenalty(User user, OffsetDateTime now);

    Optional<UserPenalty> findActivePenaltyByUser(User user, OffsetDateTime now);

    List<UserPenalty> findPenaltyHistoryByUser(User user);

    Page<UserPenalty> findAllPenaltiesWithFilters(PenaltyType type, Pageable pageable);
}
