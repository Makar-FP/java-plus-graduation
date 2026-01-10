package ru.practicum.eventservice.request.mapper;

import ru.practicum.eventservice.dto.request.RequestDto;
import ru.practicum.eventservice.dto.request.RequestEventDto;
import ru.practicum.eventservice.dto.user.UserRequestDto;
import ru.practicum.eventservice.event.model.Event;
import ru.practicum.eventservice.request.model.Request;
import ru.practicum.eventservice.request.model.RequestStatus;

import java.time.LocalDateTime;

public class RequestMapper {
    public static Request toEntity(UserRequestDto requester, Event event, RequestStatus status) {
        Request request = new Request();
        request.setEventId(event.getId());
        request.setRequesterId(requester.getId());
        request.setCreated(LocalDateTime.now());
        request.setStatus(status);

        return request;
    }

    public static RequestDto toDto(Request request) {
        return new RequestDto(
                request.getId(),
                request.getCreated(),
                request.getRequesterId(),
                request.getEventId(),
                request.getStatus()
        );
    }

    public static RequestEventDto toEventRequestDto(Request request) {
        return new RequestEventDto(
                request.getId(),
                request.getCreated(),
                request.getRequesterId(),
                request.getEventId(),
                request.getStatus()
        );
    }

    public static RequestEventDto toEventRequestDtoInt(RequestDto request) {
        return new RequestEventDto(
                request.getId(),
                request.getCreated(),
                request.getRequester(),
                request.getEvent(),
                request.getStatus()
        );
    }

}
