package ru.practicum.eventservice.event.service;

import com.querydsl.core.types.dsl.BooleanExpression;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.practicum.client.CollectorClient;
import ru.practicum.client.RecommendationClient;
import ru.practicum.eventservice.category.model.Category;
import ru.practicum.eventservice.category.model.QCategory;
import ru.practicum.eventservice.category.repo.CategoryRepository;
import ru.practicum.eventservice.client.RequestClient;
import ru.practicum.eventservice.client.UserClient;
import ru.practicum.eventservice.dto.event.*;
import ru.practicum.eventservice.dto.request.RequestEventDto;
import ru.practicum.eventservice.dto.user.UserRequestDto;
import ru.practicum.eventservice.event.mapper.EventMapper;
import ru.practicum.eventservice.event.model.*;
import ru.practicum.eventservice.event.repo.EventRepository;
import ru.practicum.eventservice.exception.*;
import ru.practicum.eventservice.request.mapper.RequestMapper;
import ru.practicum.eventservice.request.model.Request;
import ru.practicum.eventservice.request.model.RequestStatus;
import ru.practicum.grpc.stats.recommendations.RecommendedEventProto;
import ru.practicum.grpc.stats.useraction.UserActionProto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@ComponentScan(value = {"ru.yandex.practicum.ewm", "ru.practicum.client"})
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final RequestClient requestClient;
    private final UserClient userClient;
    private final EventMapper mapper;
    private final CollectorClient grpcUserActionClient;
    private final RecommendationClient grpcEventSimilarityClient;

    @Override
    public List<EventFullDto> getAdmin(AdminEventParams params) {

        PageRequest pageRequest = PageRequest.of(
                params.getFrom() > 0 ? params.getFrom() / params.getSize() : 0,
                params.getSize()
        );

        BooleanExpression filter = byStates(params.getStates())
                .and(byCategoryIds(params.getCategories()))
                .and(byUserIds(params.getUsers()))
                .and(byDates(params.getRangeStart(), params.getRangeEnd()));

        Page<Event> pageEvents = eventRepository.findAll(filter, pageRequest);
        List<Event> foundEvents = pageEvents.getContent();

        List<Long> userIds = foundEvents.stream().map(Event::getInitiatorId).toList();
        Map<Long, UserRequestDto> users = userClient.getUsersById(userIds)
                .stream()
                .collect(Collectors.toMap(UserRequestDto::getId, u -> u));

        return foundEvents.stream()
                .map(e -> mapper.toEventFullDto(e, users.get(e.getInitiatorId())))
                .toList();
    }

    @Override
    public List<EventShortDto> getPublic(PublicEventParams params) {

        PageRequest pageRequest;
        if (params.getSort() != null) {
            if (params.getSort().equals(EventPublicSort.EVENT_DATE)) {
                pageRequest = PageRequest.of(
                        params.getFrom() > 0 ? params.getFrom() / params.getSize() : 0,
                        params.getSize(),
                        Sort.by("eventDate")
                );
            } else if (params.getSort().equals(EventPublicSort.RATING)) {
                pageRequest = PageRequest.of(
                        params.getFrom() > 0 ? params.getFrom() / params.getSize() : 0,
                        params.getSize(),
                        Sort.by("rating")
                );
            } else {
                pageRequest = PageRequest.of(
                        params.getFrom() > 0 ? params.getFrom() / params.getSize() : 0,
                        params.getSize()
                );
            }
        } else {
            pageRequest = PageRequest.of(
                    params.getFrom() > 0 ? params.getFrom() / params.getSize() : 0,
                    params.getSize()
            );
        }

        BooleanExpression filter = byPublishedEvents()
                .and(byText(params.getText()))
                .and(byCategoryIds(params.getCategories()))
                .and(byPaid(params.getPaid()))
                .and(byOnlyAvailable(params.getOnlyAvailable()))
                .and(byDatesWithDefaults(params.getRangeStart(), params.getRangeEnd()));

        Page<Event> pageEvents = eventRepository.findAll(filter, pageRequest);
        List<Event> foundEvents = pageEvents.getContent();
        if (foundEvents.isEmpty()) {
            throw new EventsGetPublicBadRequestException();
        }

        List<Long> userIds = foundEvents.stream().map(Event::getInitiatorId).toList();
        Map<Long, UserRequestDto> users = userClient.getUsersById(userIds)
                .stream()
                .collect(Collectors.toMap(UserRequestDto::getId, u -> u));

        return foundEvents.stream()
                .map(e -> mapper.toEventShortDto(e, users.get(e.getInitiatorId())))
                .toList();
    }

    @Override
    public List<EventShortDto> getPrivate(PrivateEventParams params) {
        UserRequestDto user = userClient.getUsersById(List.of(params.getUserId())).getFirst();
        if (user == null) {
            throw new UserNotFoundException(params.getUserId());
        }

        PageRequest pageRequest = PageRequest.of(
                params.getFrom() > 0 ? params.getFrom() / params.getSize() : 0,
                params.getSize()
        );

        BooleanExpression filter = byUserIds(Set.of(params.getUserId()));
        Page<Event> pageEvents = eventRepository.findAll(filter, pageRequest);
        List<Event> foundEvents = pageEvents.getContent();

        List<Long> userIds = foundEvents.stream().map(Event::getInitiatorId).toList();
        Map<Long, UserRequestDto> users = userClient.getUsersById(userIds)
                .stream()
                .collect(Collectors.toMap(UserRequestDto::getId, u -> u));

        return foundEvents.stream()
                .map(e -> mapper.toEventShortDto(e, users.get(e.getInitiatorId())))
                .toList();
    }

    @Override
    @Transactional
    public EventFullDto getByIdPublic(Long userId, Long eventId, PublicEventParams params) {
        Optional<Event> event = eventRepository.findById(eventId);
        if (event.isEmpty() || !event.get().getState().equals(EventState.PUBLISHED)) {
            throw new EventNotFoundException(eventId);
        }

        UserRequestDto user = userClient.getUsersById(List.of(userId)).getFirst();
        if (user == null) {
            throw new UserNotFoundException(userId);
        }

        Instant now = Instant.now();
        grpcUserActionClient.sendUserActionToCollector(UserActionProto.newBuilder()
                .setUserId(userId.intValue())
                .setEventId(eventId.intValue())
                .setActionTypeValue(0)
                .setTimestamp(com.google.protobuf.Timestamp.newBuilder()
                        .setSeconds(now.getEpochSecond())
                        .setNanos(now.getNano())
                        .build())
                .build()
        );

        return mapper.toEventFullDto(event.get(), user);
    }

    @Override
    public EventFullDto getByIdPrivate(Long userId, Long eventId) {
        UserRequestDto user = userClient.getUsersById(List.of(userId)).getFirst();
        if (user == null) {
            throw new UserNotFoundException(userId);
        }

        Optional<Event> event = eventRepository.findById(eventId);
        if (event.isEmpty()) {
            throw new EventNotFoundException(eventId);
        }

        if (!Objects.equals(event.get().getInitiatorId(), userId)) {
            throw new EventGetBadRequestException(eventId, userId);
        }

        return mapper.toEventFullDto(event.get(), user);
    }

    @Override
    public EventFullDto getByIdInternal(Long eventId) {
        Optional<Event> event = eventRepository.findById(eventId);
        if (event.isEmpty()) {
            throw new EventNotFoundException(eventId);
        }

        Long userId = event.get().getInitiatorId();
        UserRequestDto user = userClient.getUsersById(List.of(userId)).getFirst();
        if (user == null) {
            throw new UserNotFoundException(userId);
        }

        return mapper.toEventFullDto(event.get(), user);
    }

    @Override
    public EventFullDto update(Long eventId, EventUpdateAdminDto eventDto) {
        Optional<Event> event = eventRepository.findById(eventId);
        if (event.isEmpty()) {
            throw new EventNotFoundException(eventId);
        }

        Optional<Category> category;
        if (eventDto.getCategory() != null && !eventDto.getCategory().equals(event.get().getCategory().getId())) {
            category = categoryRepository.findById(eventDto.getCategory());
            if (category.isEmpty()) {
                throw new CategoryNotFoundException(eventDto.getCategory());
            }
        } else {
            category = Optional.of(event.get().getCategory());
        }

        if (eventDto.getEventDate() != null && eventDto.getEventDate().isBefore(LocalDateTime.now())) {
            throw new EventDateException("The event date cannot be moved to the past.");
        }

        if (eventDto.getEventDate() != null
                && event.get().getPublishedOn() != null
                && eventDto.getEventDate().isBefore(event.get().getPublishedOn().minus(1, ChronoUnit.HOURS))) {
            throw new DataIntegrityViolationException(
                    "The start date of the updated event must be at least one hour from the publication time."
            );
        }

        if (eventDto.getStateAction() != null
                && eventDto.getStateAction().equals(EventStateAction.PUBLISH_EVENT)
                && !event.get().getState().equals(EventState.PENDING)) {
            throw new DataIntegrityViolationException(
                    "An event can be published only if it is in the PENDING state."
            );
        }

        if (eventDto.getStateAction() != null
                && eventDto.getStateAction().equals(EventStateAction.REJECT_EVENT)
                && event.get().getState().equals(EventState.PUBLISHED)) {
            throw new DataIntegrityViolationException(
                    "An event can be rejected only if it has not been published yet."
            );
        }

        Event updEvent = mapper.toEventFromUpdateAdmin(eventDto, category.get(), event.get());
        updEvent = eventRepository.save(updEvent);

        Long userId = updEvent.getInitiatorId();
        UserRequestDto user = userClient.getUsersById(List.of(userId)).getFirst();
        if (user == null) {
            throw new UserNotFoundException(userId);
        }

        return mapper.toEventFullDto(updEvent, user);
    }

    @Override
    public EventFullDto updateInternal(Long eventId, EventFullDto eventDto) {
        Optional<Event> event = eventRepository.findById(eventId);
        if (event.isEmpty()) {
            throw new EventNotFoundException(eventId);
        }

        Event updEvent = mapper.toEventFromEventFullDto(eventDto);
        updEvent.setId(eventId);
        updEvent = eventRepository.save(updEvent);

        Long userId = updEvent.getInitiatorId();
        UserRequestDto user = userClient.getUsersById(List.of(userId)).getFirst();
        return mapper.toEventFullDto(updEvent, user);
    }

    @Override
    public EventFullDto updatePrivate(Long userId, Long eventId, EventUpdateUserDto eventDto) {
        UserRequestDto user = userClient.getUsersById(List.of(userId)).getFirst();
        if (user == null) {
            throw new UserNotFoundException(userId);
        }

        Optional<Event> event = eventRepository.findById(eventId);
        if (event.isEmpty()) {
            throw new EventNotFoundException(eventId);
        }

        Optional<Category> category;
        if (eventDto.getCategory() != null && !eventDto.getCategory().equals(event.get().getCategory().getId())) {
            category = categoryRepository.findById(eventDto.getCategory());
            if (category.isEmpty()) {
                throw new CategoryNotFoundException(eventDto.getCategory());
            }
        } else {
            category = Optional.of(event.get().getCategory());
        }

        if (!Objects.equals(event.get().getInitiatorId(), userId)) {
            throw new EventGetBadRequestException(eventId, userId);
        }

        if (eventDto.getEventDate() != null && eventDto.getEventDate().minusHours(2).isBefore(LocalDateTime.now())) {
            throw new EventDateException("The event date and time must be at least 2 hours from now.");
        }

        if (event.get().getState().equals(EventState.PUBLISHED)) {
            throw new DataIntegrityViolationException(
                    "You can update only canceled events or events waiting for moderation."
            );
        }

        Event updEvent = mapper.toEventFromUpdateUser(eventDto, category.get(), event.get());
        updEvent = eventRepository.save(updEvent);
        return mapper.toEventFullDto(updEvent, user);
    }

    @Override
    public EventFullDto create(Long userId, EventCreateDto eventDto) {
        UserRequestDto user = userClient.getUsersById(List.of(userId)).getFirst();
        if (user == null) {
            throw new UserNotFoundException(userId);
        }

        Optional<Category> category = categoryRepository.findById(eventDto.getCategory());
        if (category.isEmpty()) {
            throw new CategoryNotFoundException(eventDto.getCategory());
        }

        Event event = mapper.toEventFromCreatedDto(eventDto, user, category.get());
        event = eventRepository.save(event);
        return mapper.toEventFullDto(event, user);
    }

    @Override
    public List<RequestEventDto> getRequestsByIdPrivate(Long userId, Long eventId) {
        UserRequestDto user = userClient.getUsersById(List.of(userId)).getFirst();
        if (user == null) {
            throw new UserNotFoundException(userId);
        }

        Optional<Event> event = eventRepository.findById(eventId);
        if (event.isEmpty()) {
            throw new EventNotFoundException(eventId);
        }

        if (!event.get().getInitiatorId().equals(userId)) {
            throw new ConflictException("You are not the owner of this event.");
        }

        List<Request> requests = requestClient.getByEventId(eventId);
        return requests.stream()
                .map(RequestMapper::toEventRequestDto)
                .toList();
    }

    @Override
    public EventResultRequestStatusDto updateRequestStatusPrivate(Long userId, Long eventId, EventUpdateRequestStatusDto updateDto) {
        UserRequestDto user = userClient.getUsersById(List.of(userId)).getFirst();
        if (user == null) {
            throw new UserNotFoundException(userId);
        }

        Optional<Event> event = eventRepository.findById(eventId);
        if (event.isEmpty()) {
            throw new EventNotFoundException(eventId);
        }

        Integer confReqs = event.get().getConfirmedRequests();
        Integer limit = event.get().getParticipantLimit();

        if (limit == 0 || !event.get().getRequestModeration()) {
            return null;
        }

        if (Objects.equals(confReqs, limit) && updateDto.getStatus().equals(RequestStatus.CONFIRMED)) {
            throw new DataIntegrityViolationException("The participant limit has been reached");
        }

        int count = limit - confReqs;
        int counter = 0;

        List<RequestEventDto> confirmedRequests = new ArrayList<>();
        List<RequestEventDto> rejectedRequests = new ArrayList<>();

        List<Request> requests = requestClient.getByEventIdAndIds(eventId, updateDto.getRequestIds());
        for (Request request : requests) {
            if (!request.getStatus().equals(RequestStatus.PENDING)) {
                throw new DataIntegrityViolationException("Request must have status PENDING");
            }

            if (updateDto.getStatus().equals(RequestStatus.CONFIRMED) && counter < count) {
                counter++;
                request.setStatus(RequestStatus.CONFIRMED);
                requestClient.updateInternal(request);
                confirmedRequests.add(RequestMapper.toEventRequestDto(request));
            } else {
                counter++;
                request.setStatus(RequestStatus.REJECTED);
                requestClient.updateInternal(request);
                rejectedRequests.add(RequestMapper.toEventRequestDto(request));
            }
        }

        event.get().setConfirmedRequests(confReqs + counter);
        eventRepository.save(event.get());

        EventResultRequestStatusDto results = new EventResultRequestStatusDto();
        results.setConfirmedRequests(confirmedRequests);
        results.setRejectedRequests(rejectedRequests);
        return results;
    }

    @Override
    public EventFullDto setLike(Long eventId, Long userId) {
        UserRequestDto user = userClient.getUsersById(List.of(userId)).getFirst();
        if (user == null) {
            throw new UserNotFoundException(userId);
        }

        Optional<Event> event = eventRepository.findById(eventId);
        if (event.isEmpty()) {
            throw new EventNotFoundException(eventId);
        }

        Instant now = Instant.now();
        grpcUserActionClient.sendUserActionToCollector(UserActionProto.newBuilder()
                .setUserId(userId.intValue())
                .setEventId(eventId.intValue())
                .setActionTypeValue(2)
                .setTimestamp(com.google.protobuf.Timestamp.newBuilder()
                        .setSeconds(now.getEpochSecond())
                        .setNanos(now.getNano())
                        .build())
                .build()
        );

        return mapper.toEventFullDto(event.get(), user);
    }

    @Override
    public List<EventFullDto> getRecommendations(Long userId, int maxResults) {
        UserRequestDto user = userClient.getUsersById(List.of(userId)).getFirst();
        if (user == null) {
            throw new UserNotFoundException(userId);
        }

        List<RecommendedEventProto> recommendedEventProtoList =
                grpcEventSimilarityClient.getRecommendationsForUser(userId, maxResults).toList();

        Set<Long> events = recommendedEventProtoList.stream()
                .map(RecommendedEventProto::getEventId)
                .collect(Collectors.toSet());

        return eventRepository.findByIdIn(events).stream()
                .map(event -> mapper.toEventFullDto(event, user))
                .toList();
    }

    @Override
    public List<RecommendedEventProto> getInteractions(Set<Long> eventsIds) {
        List<Long> events = new ArrayList<>(eventsIds);
        return grpcEventSimilarityClient.getInteractionsCount(events).toList();
    }

    @Override
    public List<EventFullDto> getSimilarEvents(Long eventId, Long userId, int maxResults) {
        UserRequestDto user = userClient.getUsersById(List.of(userId)).getFirst();
        if (user == null) {
            throw new UserNotFoundException(userId);
        }

        List<RecommendedEventProto> similarEventsProtoList =
                grpcEventSimilarityClient.getSimilarEvents(eventId, userId, maxResults).toList();

        Set<Long> events = similarEventsProtoList.stream()
                .map(RecommendedEventProto::getEventId)
                .collect(Collectors.toSet());

        return eventRepository.findByIdIn(events).stream()
                .map(event -> mapper.toEventFullDto(event, user))
                .toList();
    }

    private BooleanExpression byStates(Set<EventState> states) {
        return states != null
                ? QEvent.event.state.in(states)
                : QEvent.event.state.in(Set.of(EventState.CANCELED, EventState.PENDING, EventState.PUBLISHED));
    }

    private BooleanExpression byCategoryIds(Set<Long> categories) {
        return categories != null && !categories.isEmpty() && categories.iterator().next() != 0
                ? QCategory.category.id.in(categories)
                : null;
    }

    private BooleanExpression byUserIds(Set<Long> users) {
        return users != null && !users.isEmpty() && users.iterator().next() != 0
                ? QEvent.event.initiatorId.in(users)
                : null;
    }

    private BooleanExpression byDates(LocalDateTime start, LocalDateTime end) {
        return start != null && end != null
                ? QEvent.event.eventDate.after(start).and(QEvent.event.eventDate.before(end))
                : null;
    }

    private BooleanExpression byDatesWithDefaults(LocalDateTime start, LocalDateTime end) {
        return start != null && end != null
                ? QEvent.event.eventDate.after(start).and(QEvent.event.eventDate.before(end))
                : QEvent.event.eventDate.after(LocalDateTime.now());
    }

    private BooleanExpression byText(String text) {
        return text != null && !text.equals("0")
                ? QEvent.event.annotation.containsIgnoreCase(text)
                : null;
    }

    private BooleanExpression byPaid(Boolean paid) {
        return paid != null ? QEvent.event.paid.eq(paid) : null;
    }

    private BooleanExpression byPublishedEvents() {
        return QEvent.event.state.eq(EventState.PUBLISHED);
    }

    private BooleanExpression byOnlyAvailable(Boolean onlyAvailable) {
        return onlyAvailable != null && onlyAvailable
                ? QEvent.event.confirmedRequests.lt(QEvent.event.participantLimit)
                : null;
    }
}