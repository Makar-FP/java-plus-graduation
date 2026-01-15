package ru.practicum.eventservice.event.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;
import ru.practicum.eventservice.event.model.Event;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface EventRepository extends JpaRepository<Event, Long>, QuerydslPredicateExecutor<Event> {
    Optional<Event> findFirstByCategoryId(Long categoryId);

    List<Event> findByIdIn(Set<Long> eventIds);
}
