package ru.practicum.stats.analyzer.dal.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.stats.analyzer.dal.model.UserAction;

import java.util.List;
import java.util.Set;

public interface InteractionsRepository extends JpaRepository<UserAction, Long> {

    UserAction findByUserIdAndEventId(Long userId, Long eventId);

    List<UserAction> findByEventIdIn(Set<Long> eventIds);

    List<UserAction> findByUserId(Long userId);

    @Query("select distinct e from UserAction e where e.eventId not in ( select u.eventId from UserAction u where u.userId = ?1)")
    List<UserAction> findNotByUserId(Long userId);

    @Query("select distinct e.eventId from UserAction e where e.eventId not in (select u.eventId from UserAction u where u.userId = :userId)")
    Set<Long> findEventIdsNotInteractedByUser(@Param("userId") Long userId);

    @Query("select e.eventId as eventId, sum(e.rating) as score from UserAction e where e.eventId in :eventIds group by e.eventId")
    List<EventScoreSum> sumRatingsByEventIds(@Param("eventIds") Set<Long> eventIds);

    @Modifying
    @Transactional
    @Query(
            value = "insert into interactions (user_id, event_id, rating, ts) " +
                    "values (:userId, :eventId, :rating, :ts) " +
                    "on conflict (user_id, event_id) do update " +
                    "set rating = excluded.rating, ts = excluded.ts " +
                    "where interactions.rating < excluded.rating",
            nativeQuery = true
    )
    int upsertIfHigher(
            @Param("userId") Long userId,
            @Param("eventId") Long eventId,
            @Param("rating") Double rating,
            @Param("ts") java.time.Instant ts
    );

    interface EventScoreSum {
        Long getEventId();
        Double getScore();
    }
}
