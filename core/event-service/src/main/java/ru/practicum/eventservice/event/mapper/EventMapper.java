package ru.practicum.eventservice.event.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.eventservice.category.model.Category;
import ru.practicum.eventservice.dto.event.*;
import ru.practicum.eventservice.dto.user.UserRequestDto;
import ru.practicum.eventservice.dto.user.UserShortDto;
import ru.practicum.eventservice.event.model.*;

@Component
@RequiredArgsConstructor
public class EventMapper {

    public EventFullDto toEventFullDto(Event event, UserRequestDto user) {
        return EventFullDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(mapCategory(event.getCategory()))
                .confirmedRequests(event.getConfirmedRequests())
                .createdOn(event.getCreatedOn())
                .description(event.getDescription())
                .eventDate(event.getEventDate())
                .initiator(mapInitiator(event.getInitiatorId(), user))
                .location(mapLocation(event.getLat(), event.getLon()))
                .paid(event.getPaid())
                .participantLimit(event.getParticipantLimit())
                .publishedOn(event.getPublishedOn())
                .requestModeration(event.getRequestModeration())
                .state(event.getState())
                .title(event.getTitle())
                .rating(event.getRating())
                .build();
    }

    public EventShortDto toEventShortDto(Event event, UserRequestDto user) {
        return EventShortDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(mapCategory(event.getCategory()))
                .confirmedRequests(event.getConfirmedRequests())
                .eventDate(event.getEventDate())
                .initiator(mapInitiator(event.getInitiatorId(), user))
                .paid(event.getPaid())
                .title(event.getTitle())
                .rating(event.getRating())
                .build();
    }

    public Event toEventFromUpdateAdmin(EventUpdateAdminDto eventDto, Category updCategory, Event curEvent) {

        if (eventDto.getAnnotation() != null) {
            curEvent.setAnnotation(eventDto.getAnnotation());
        }
        curEvent.setCategory(updCategory);

        if (eventDto.getDescription() != null) {
            curEvent.setDescription(eventDto.getDescription());
        }
        if (eventDto.getEventDate() != null) {
            curEvent.setEventDate(eventDto.getEventDate());
        }
        if (eventDto.getLocation() != null) {
            curEvent.setLat(eventDto.getLocation().getLat());
            curEvent.setLon(eventDto.getLocation().getLon());
        }
        if (eventDto.getPaid() != null) {
            curEvent.setPaid(eventDto.getPaid());
        }
        if (eventDto.getParticipantLimit() != null) {
            curEvent.setParticipantLimit(eventDto.getParticipantLimit());
        }
        if (eventDto.getStateAction() != null && eventDto.getStateAction().equals(EventStateAction.PUBLISH_EVENT)) {
            curEvent.setState(EventState.PUBLISHED);
        }
        if (eventDto.getStateAction() != null && eventDto.getStateAction().equals(EventStateAction.REJECT_EVENT)) {
            curEvent.setState(EventState.CANCELED);
        }
        if (eventDto.getTitle() != null) {
            curEvent.setTitle(eventDto.getTitle());
        }

        return curEvent;
    }

    public Event toEventFromUpdateUser(EventUpdateUserDto eventDto, Category updCategory, Event curEvent) {

        if (eventDto.getAnnotation() != null) {
            curEvent.setAnnotation(eventDto.getAnnotation());
        }
        curEvent.setCategory(updCategory);

        if (eventDto.getDescription() != null) {
            curEvent.setDescription(eventDto.getDescription());
        }
        if (eventDto.getEventDate() != null) {
            curEvent.setEventDate(eventDto.getEventDate());
        }
        if (eventDto.getLocation() != null) {
            curEvent.setLat(eventDto.getLocation().getLat());
            curEvent.setLon(eventDto.getLocation().getLon());
        }
        if (eventDto.getParticipantLimit() != null) {
            curEvent.setParticipantLimit(eventDto.getParticipantLimit());
        }
        if (eventDto.getStateAction() != null && eventDto.getStateAction().equals(EventUserStateAction.SEND_TO_REVIEW)) {
            curEvent.setState(EventState.PENDING);
        }
        if (eventDto.getStateAction() != null && eventDto.getStateAction().equals(EventUserStateAction.CANCEL_REVIEW)) {
            curEvent.setState(EventState.CANCELED);
        }
        if (eventDto.getTitle() != null) {
            curEvent.setTitle(eventDto.getTitle());
        }

        return curEvent;
    }

    public Event toEventFromCreatedDto(EventCreateDto eventDto, UserRequestDto user, Category category) {
        return Event.builder()
                .annotation(eventDto.getAnnotation())
                .category(category)
                .description(eventDto.getDescription())
                .createdOn(eventDto.getCreated())
                .eventDate(eventDto.getEventDate())
                .lat(eventDto.getLocation().getLat())
                .lon(eventDto.getLocation().getLon())
                .paid(eventDto.getPaid())
                .participantLimit(eventDto.getParticipantLimit())
                .requestModeration(eventDto.getRequestModeration())
                .initiatorId(user.getId())
                .title(eventDto.getTitle())
                .confirmedRequests(0)
                .state(EventState.PENDING)
                .rating(eventDto.getRating())
                .build();
    }

    public Event toEventFromEventFullDto(EventFullDto eventDto) {
        return Event.builder()
                .annotation(eventDto.getAnnotation())
                .category(eventDto.getCategory())
                .description(eventDto.getDescription())
                .createdOn(eventDto.getCreatedOn())
                .eventDate(eventDto.getEventDate())
                .lat(eventDto.getLocation().getLat())
                .lon(eventDto.getLocation().getLon())
                .paid(eventDto.getPaid())
                .participantLimit(eventDto.getParticipantLimit())
                .requestModeration(eventDto.getRequestModeration())
                .initiatorId(eventDto.getInitiator().getId())
                .title(eventDto.getTitle())
                .confirmedRequests(eventDto.getConfirmedRequests())
                .state(eventDto.getState())
                .rating(eventDto.getRating())
                .build();
    }

    private Category mapCategory(Category category) {
        if (category == null) {
            return null;
        }
        return new Category(category.getId(), category.getName());
    }

    private UserShortDto mapInitiator(Long initiatorId, UserRequestDto user) {
        return new UserShortDto(initiatorId, user.getName());
    }

    private Location mapLocation(Float lat, Float lon) {
        return new Location(lat, lon);
    }
}