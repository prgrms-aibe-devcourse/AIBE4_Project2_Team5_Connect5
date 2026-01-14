package kr.eolmago.repository.user;

import kr.eolmago.domain.entity.user.User;
import kr.eolmago.domain.entity.user.UserPenalty;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface UserPenaltyRepositoryCustom {
    List<UserPenalty> findExpiredPenalties(OffsetDateTime now);
    boolean existsActivePenalty(User user, OffsetDateTime now);
    Optional<UserPenalty> findActivePenaltyByUser(User user, OffsetDateTime now);
    List<UserPenalty> findPenaltyHistoryByUser(User user);
}
