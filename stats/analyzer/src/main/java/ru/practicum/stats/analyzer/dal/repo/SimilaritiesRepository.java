package ru.practicum.stats.analyzer.dal.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.stats.analyzer.dal.model.EventSimilarity;

import java.util.List;
import java.util.Set;

public interface SimilaritiesRepository extends JpaRepository<EventSimilarity, Long> {

    EventSimilarity findByEventAAndEventB(Long eventA, Long eventB);

    @Query("select u from EventSimilarity u where u.eventA = ?1 or u.eventB = ?1")
    List<EventSimilarity> findByEventAOrEventB(Long eventId);

    @Modifying
    @Transactional
    @Query(
            value = "insert into similarities (event1, event2, similarity, ts) " +
                    "values (:eventA, :eventB, :similarity, :ts) " +
                    "on conflict (event1, event2) do update " +
                    "set similarity = excluded.similarity, ts = excluded.ts",
            nativeQuery = true
    )
    int upsert(
            @Param("eventA") Long eventA,
            @Param("eventB") Long eventB,
            @Param("similarity") Double similarity,
            @Param("ts") java.time.Instant ts
    );

    @Query(
            "select s from EventSimilarity s " +
                    "where (s.eventA in :left and s.eventB in :right) " +
                    "   or (s.eventB in :left and s.eventA in :right)"
    )
    List<EventSimilarity> findBetweenEventSets(
            @Param("left") Set<Long> left,
            @Param("right") Set<Long> right
    );
}
