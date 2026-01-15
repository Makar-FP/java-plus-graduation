package ru.practicum.stats.analyzer.dal.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.stats.analyzer.dal.model.EventSimilarity;
import ru.practicum.stats.analyzer.dal.repo.SimilaritiesRepository;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventSimilarityService {
    private final SimilaritiesRepository repository;

    public void handleRecord(EventSimilarityAvro record) throws InterruptedException {
        Long eventA = (long) record.getEventA();
        Long eventB = (long) record.getEventB();
        Double score = record.getScore();
        Instant ts = record.getTimestamp();
        EventSimilarity dbRecord;

        if (eventA != eventB) {
            if (eventA > eventB) {
                log.info("Looking up a record in similarities table by event1={} and event2={}.", eventB, eventA);
                dbRecord = repository.findByEventAAndEventB(eventB, eventA);
            } else {
                log.info("Looking up a record in similarities table by event1={} and event2={}.", eventA, eventB);
                dbRecord = repository.findByEventAAndEventB(eventA, eventB);
            }

            EventSimilarity result = new EventSimilarity();
            if (dbRecord == null) {
                result.setEventA(Math.min(eventA, eventB));
                result.setEventB(Math.max(eventA, eventB));
                result.setSimilarity(record.getScore());
                result.setTs(record.getTimestamp());
                repository.save(result);
                log.info("Adding a new record \"{}\" to the similarities table.", result);
            } else {
                result.setId(dbRecord.getId());
                result.setEventA(dbRecord.getEventA());
                result.setEventB(dbRecord.getEventB());
                result.setSimilarity(record.getScore());
                result.setTs(record.getTimestamp());
                repository.save(result);
                log.info("Updating the record with id={} in the similarities table. Record: {}", dbRecord.getId(), result);
            }
        } else {
            log.info("event1={} and event2={} identifiers are the same.", eventA, eventB);
        }
    }
}