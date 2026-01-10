package ru.practicum.requestservice.mapper;

import ru.practicum.requestservice.dto.RequestDto;
import ru.practicum.requestservice.dto.RequestEventDto;
import ru.practicum.requestservice.dto.UserRequestDto;
import ru.practicum.requestservice.model.Event;
import ru.practicum.requestservice.model.Request;
import ru.practicum.requestservice.model.RequestStatus;

import java.time.LocalDateTime;

public class RequestMapper {

    public static Request toEntity(UserRequestDto requester, Event event, RequestStatus status) {
        return Request.builder()
                .eventId(event.getId())
                .requesterId(requester.getId())
                .created(LocalDateTime.now())
                .status(status)
                .build();
    }

    public static RequestDto toDto(Request request) {
        return RequestDto.builder()
                .id(request.getId())
                .created(request.getCreated())
                .requester(request.getRequesterId())
                .event(request.getEventId())
                .status(request.getStatus())
                .build();
    }

    public static RequestEventDto toEventRequestDto(Request request) {
        return RequestEventDto.builder()
                .id(request.getId())
                .created(request.getCreated())
                .requester(request.getRequesterId())
                .event(request.getEventId())
                .status(request.getStatus())
                .build();
    }

    public static Request toRequest(RequestDto requestDto) {
        return Request.builder()
                .id(requestDto.getId())
                .created(requestDto.getCreated())
                .requesterId(requestDto.getRequester())
                .eventId(requestDto.getEvent())
                .status(requestDto.getStatus())
                .build();
    }
}
