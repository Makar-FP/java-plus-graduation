package ru.practicum.eventservice.event.service;

import ru.practicum.eventservice.dto.event.*;
import ru.practicum.eventservice.dto.request.RequestEventDto;
import ru.practicum.eventservice.event.model.AdminEventParams;
import ru.practicum.eventservice.event.model.PrivateEventParams;
import ru.practicum.eventservice.event.model.PublicEventParams;

import java.util.List;

public interface EventService {
        List<EventFullDto> getAdmin(AdminEventParams params);

        List<EventShortDto> getPublic(PublicEventParams params);

        List<EventShortDto> getPrivate(PrivateEventParams params);

        EventFullDto getByIdPublic(Long eventId, PublicEventParams params);

        EventFullDto getByIdPrivate(Long userId, Long eventId);

        EventFullDto getByIdInternal(Long eventId);

        EventFullDto update(Long eventId, EventUpdateAdminDto eventDto);

        EventFullDto updateInternal(Long eventId, EventFullDto eventDto);

        EventFullDto updatePrivate(Long userId, Long eventId, EventUpdateUserDto eventUpdateDto);

        EventFullDto create(Long userId, EventCreateDto eventDto);

        List<RequestEventDto> getRequestsByIdPrivate(Long userId, Long eventId);

        EventResultRequestStatusDto updateRequestStatusPrivate(Long userId, Long eventId, EventUpdateRequestStatusDto updateDto);
}
