package ru.yandex.practicum.ewm.event;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.ewm.event.dto.EventFullDto;
import ru.yandex.practicum.ewm.event.dto.EventShortDto;
import ru.yandex.practicum.ewm.event.model.EventPublicSort;
import ru.yandex.practicum.ewm.event.model.PublicEventParams;
import ru.yandex.practicum.ewm.event.service.EventService;

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
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeStart,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeEnd,
            @RequestParam(defaultValue = "false") Boolean onlyAvailable,
            @RequestParam(required = false) EventPublicSort sort,
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request
    ) {
        PublicEventParams params = new PublicEventParams();
        params.setText(text);
        params.setCategories(categories);
        params.setPaid(paid);
        params.setRangeStart(rangeStart);
        params.setRangeEnd(rangeEnd);
        params.setOnlyAvailable(onlyAvailable);
        params.setSort(sort);
        params.setFrom(from);
        params.setSize(size);
        params.setIpAdr(resolveClientIp(request));

        log.info("--> GET /events params={}", params);
        List<EventShortDto> events = eventService.getPublic(params);
        log.info("<-- GET /events returned {} items", events == null ? 0 : events.size());

        return ResponseEntity
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(events);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventFullDto> getById(
            @PathVariable("id") Long eventId,
            HttpServletRequest request
    ) {
        PublicEventParams params = new PublicEventParams();
        params.setIpAdr(resolveClientIp(request));

        log.info("--> GET /events/{} ip={}", eventId, params.getIpAdr());
        EventFullDto event = eventService.getByIdPublic(eventId, params);
        log.info("<-- GET /events/{} returned", eventId);

        return ResponseEntity
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(event);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
