package ru.practicum.ewm.stats.analyzer.dal.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.stats.analyzer.dal.model.EventSimilarity;

import java.util.List;
import java.util.Optional;

public interface EventSimilarityRepository extends JpaRepository<EventSimilarity, Long> {
    Optional<EventSimilarity> findByEventAAndEventB(long eventA, long eventB);

    @Query("SELECT es FROM EventSimilarity es WHERE es.eventA = :eventId OR es.eventB = :eventId")
    List<EventSimilarity> findAllByEventId(@Param("eventId") long eventId);

    @Query("SELECT es FROM EventSimilarity es " +
        "WHERE ((es.eventA IN :eventIds OR es.eventB IN :eventIds) " +
        "AND NOT (es.eventA IN :eventIds AND es.eventB IN :eventIds))")
    List<EventSimilarity> findNewSimilar(@Param("eventIds") List<Long> eventIds, Pageable pageable);

    @Query("SELECT es FROM EventSimilarity es WHERE es.eventA = :eventId OR es.eventB = :eventId")
    List<EventSimilarity> findNeighbours(@Param("eventId") long eventId, Pageable pageable);
}
