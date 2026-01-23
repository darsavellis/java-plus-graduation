package ru.practicum.ewm.stats.analyzer.dal.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.ewm.stats.analyzer.dal.model.UserAction;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserActionRepository extends JpaRepository<UserAction, Long> {
    Optional<UserAction> findByUserIdAndEventId(long userId, long eventId);

    @Query("SELECT au.eventId FROM UserAction au WHERE au.userId = :userId AND au.eventId <> :eventId")
    Set<Long> findByUserIdExcludeEventId(long userId, long eventId);

    @Query("SELECT au.eventId FROM UserAction au WHERE au.userId = :userId")
    List<Long> findByUserId(long userId, Pageable pageable);

    List<UserAction> findByEventIdIn(Set<Long> eventId);
}
