package ru.practicum.eventservice.event.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.eventservice.dto.event.EventFullDto;
import ru.practicum.eventservice.dto.event.EventShortDto;
import ru.practicum.eventservice.event.model.EventPublicSort;
import ru.practicum.eventservice.event.model.PublicEventParams;
import ru.practicum.eventservice.event.service.EventService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/events")
public class EventPublicController {
    private final EventService eventService;

    @GetMapping
    public ResponseEntity<List<EventShortDto>> get(
            @RequestParam(required = false) String text,
            @RequestParam(required = false) Set<Long> categories,
            @RequestParam(required = false) Boolean paid,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeStart,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeEnd,
            @RequestParam(defaultValue = "false") Boolean onlyAvailable,
            @RequestParam(required = false) EventPublicSort sort,
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {

        PublicEventParams publicEventParams = new PublicEventParams();
        publicEventParams.setText(text);
        publicEventParams.setCategories(categories);
        publicEventParams.setPaid(paid);
        publicEventParams.setRangeStart(rangeStart);
        publicEventParams.setRangeEnd(rangeEnd);
        publicEventParams.setOnlyAvailable(onlyAvailable);
        publicEventParams.setSort(sort);
        publicEventParams.setFrom(from);
        publicEventParams.setSize(size);
        publicEventParams.setIpAdr(request.getRemoteAddr());

        log.info("--> GET /events request with params {}", publicEventParams);
        List<EventShortDto> events = eventService.getPublic(publicEventParams);
        log.info("<-- GET /events response: {}", events);

        return ResponseEntity
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(events);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventFullDto> getById(
            @RequestHeader(value = "X-EWM-USER-ID", required = false) Long userId,
            @PathVariable("id") Long eventId,
            HttpServletRequest request) {

        PublicEventParams publicEventParams = new PublicEventParams();
        publicEventParams.setIpAdr(request.getRemoteAddr());

        log.info("--> GET /events/{} request, userId={}", eventId, userId);
        EventFullDto event = eventService.getByIdPublic(userId, eventId, publicEventParams);
        log.info("<-- GET /events/{} response: {}", eventId, event);

        return ResponseEntity
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(event);
    }
}