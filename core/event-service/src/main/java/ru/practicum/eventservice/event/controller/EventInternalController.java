package ru.practicum.eventservice.event.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.eventservice.dto.event.EventFullDto;
import ru.practicum.eventservice.event.service.EventService;
import ru.practicum.grpc.stats.recommendations.RecommendedEventProto;

import java.util.List;
import java.util.Set;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/events")
public class EventInternalController {
    private final EventService eventService;

    @PutMapping("/{id}")
    public ResponseEntity<EventFullDto> updateInternal(@RequestBody @Valid EventFullDto eventUpdateDto,
                                                       @PathVariable("id") Long eventId) {

        log.info("--> PUT /events/{} request body: {}", eventId, eventUpdateDto);
        EventFullDto event = eventService.updateInternal(eventId, eventUpdateDto);
        log.info("<-- PUT /events/{} response: {}", eventId, event);

        return ResponseEntity
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(event);
    }

    @GetMapping("/{id}/internal")
    public ResponseEntity<EventFullDto> getByIdInternal(@PathVariable("id") Long eventId) {

        log.info("--> GET /events/{}/internal request", eventId);
        EventFullDto event = eventService.getByIdInternal(eventId);
        log.info("<-- GET /events/{}/internal response: {}", eventId, event);

        return ResponseEntity
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(event);
    }

    @PutMapping("/{id}/like")
    public ResponseEntity<EventFullDto> setLike(@RequestHeader("X-EWM-USER-ID") long userId,
                                                @PathVariable("id") Long eventId) {

        log.info("--> PUT /events/{}/like request for user {}", eventId, userId);
        EventFullDto event = eventService.setLike(eventId, userId);
        log.info("<-- PUT /events/{}/like response: {}", eventId, event);

        return ResponseEntity
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(event);
    }

    @GetMapping("/{id}/similar")
    public ResponseEntity<List<EventFullDto>> getSimilarEvents(@RequestHeader("X-EWM-USER-ID") long userId,
                                                               @PathVariable("id") Long eventId,
                                                               @RequestParam(defaultValue = "10") int maxResults) {

        log.info("--> GET /events/{}/similar request for user {}", eventId, userId);
        List<EventFullDto> events = eventService.getSimilarEvents(eventId, userId, maxResults);
        log.info("<-- GET /events/{}/similar response: {}", eventId, events);

        return ResponseEntity
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(events);
    }

    @GetMapping("/recommendations")
    public ResponseEntity<List<EventFullDto>> getRecommendations(@RequestHeader("X-EWM-USER-ID") long userId,
                                                                 @RequestParam(defaultValue = "10") int maxResults) {

        log.info("--> GET /events/recommendations request for user {}", userId);
        List<EventFullDto> events = eventService.getRecommendations(userId, maxResults);
        log.info("<-- GET /events/recommendations response: {}", events);

        return ResponseEntity
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(events);
    }

    @GetMapping("/interactions")
    public ResponseEntity<List<Double>> getInteractions(@RequestParam(required = false) Set<Long> eventsIds) {

        log.info("--> GET /events/interactions request, eventsIds={}", eventsIds);
        List<RecommendedEventProto> results = eventService.getInteractions(eventsIds);
        log.info("<-- GET /events/interactions response, eventsIds={}, results={}", eventsIds, results);

        List<Double> scores = results.stream()
                .map(RecommendedEventProto::getScore)
                .toList();

        return ResponseEntity
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(scores);
    }
}