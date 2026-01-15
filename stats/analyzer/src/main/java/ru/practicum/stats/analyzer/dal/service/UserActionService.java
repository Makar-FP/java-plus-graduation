package ru.practicum.stats.analyzer.dal.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.stats.analyzer.dal.repo.InteractionsRepository;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserActionService {
    private final InteractionsRepository repository;

    public void handleRecord(UserActionAvro record) {

        Long userId = (long) record.getUserId();
        Long eventId = (long) record.getEventId();

        Double rating = switch (record.getActionType().toString()) {
            case "ACTION_LIKE" -> 1.0;
            case "ACTION_REGISTER" -> 0.8;
            case "ACTION_VIEW" -> 0.4;
            default -> 0.0;
        };

        Instant ts = record.getTimestamp();

        int affected = repository.upsertIfHigher(userId, eventId, rating, ts);
        log.debug("UPSERT interactions userId={}, eventId={}, affectedRows={}", userId, eventId, affected);
    }
}
