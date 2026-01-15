package ru.practicum.requestservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.requestservice.dto.EventFullDto;

@FeignClient(name = "event-service")
public interface EventClient {

    @GetMapping("/events/{id}/internal")
    EventFullDto getByIdInternal(@PathVariable("id") Long eventId);

    @PutMapping("/events/{id}")
    EventFullDto updateInternal(@RequestBody EventFullDto eventUpdateDto, @PathVariable("id") Long eventId);
}

