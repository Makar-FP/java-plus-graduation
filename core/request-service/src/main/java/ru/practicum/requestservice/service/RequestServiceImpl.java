package ru.practicum.requestservice.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.requestservice.client.EventClient;
import ru.practicum.requestservice.client.UserClient;
import ru.practicum.requestservice.dto.EventFullDto;
import ru.practicum.requestservice.dto.UserRequestDto;
import ru.practicum.requestservice.exception.*;
import ru.practicum.requestservice.dto.RequestDto;
import ru.practicum.requestservice.mapper.RequestMapper;
import ru.practicum.requestservice.model.EventState;
import ru.practicum.requestservice.model.Request;
import ru.practicum.requestservice.model.RequestStatus;
import ru.practicum.requestservice.repo.RequestRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class RequestServiceImpl implements RequestService {

    private final RequestRepository requestRepository;
    private final UserClient userClient;
    private final EventClient eventClient;

    @Override
    public RequestDto create(Long userId, Long eventId) {
        UserRequestDto user = getUserOrThrow(userId);

        EventFullDto event;
        try {
            event = eventClient.getByIdInternal(eventId);
        } catch (feign.FeignException.NotFound e) {
            throw new EventNotFoundException(eventId);
        }

        if (event == null) {
            throw new EventNotFoundException(eventId);
        }

        if (userId.equals(event.getInitiator().getId())) {
            throw new ConflictException("You cannot register for your own event.");
        }

        if (!EventState.PUBLISHED.equals(event.getState())) {
            throw new ConflictException("You cannot register in an unpublished event.");
        }

        if (requestRepository.existsByRequesterIdAndEventId(userId, eventId)) {
            throw new ConflictException("You cannot add duplicate request.");
        }

        int confirmed = event.getConfirmedRequests() == null ? 0 : event.getConfirmedRequests();
        int limit = event.getParticipantLimit() == null ? 0 : event.getParticipantLimit();

        if (limit != 0 && confirmed >= limit) {
            throw new ConflictException("All spots are taken, registration is not possible.");
        }

        RequestStatus status = (limit == 0 || Boolean.FALSE.equals(event.getRequestModeration()))
                ? RequestStatus.CONFIRMED
                : RequestStatus.PENDING;

        Request request = new Request();
        request.setRequesterId(userId);
        request.setEventId(eventId);
        request.setCreated(LocalDateTime.now());
        request.setStatus(status);

        Request saved = requestRepository.save(request);

        if (RequestStatus.CONFIRMED.equals(status)) {
            event.setConfirmedRequests(confirmed + 1);
            eventClient.updateInternal(event, eventId);
        }

        return RequestMapper.toDto(saved);
    }


    @Override
    public List<RequestDto> get(Long userId) {
        UserRequestDto user = getUserOrThrow(userId);
        if (user == null) {
            throw new UserNotFoundException(userId);
        }

        List<Request> requests = requestRepository.findAllByRequesterId(userId);

        return requests.stream()
                .map(RequestMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public RequestDto update(Long userId, Long requestId) {
        getUserOrThrow(userId);

        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RequestNotFoundException(requestId));

        if (!userId.equals(request.getRequesterId())) {
            throw new RequestNotFoundException(requestId);
        }

        if (RequestStatus.CONFIRMED.equals(request.getStatus())) {
            throw new ConflictException("You cannot cancel an accepted request.");
        }

        request.setStatus(RequestStatus.CANCELED);
        return RequestMapper.toDto(requestRepository.save(request));
    }


    @Override
    public Request updateInternal(Request request) {
        return requestRepository.save(request);
    }

    @Override
    public List<Request> getByEventId(Long eventId) {
        return requestRepository.findAllByEventId(eventId);
    }

    @Override
    public List<Request> getByEventIdAndIds(Long eventId, Set<Long> requestsIds) {
        return requestRepository.findAllByEventIdAndIdIn(eventId, requestsIds);

    }

    private UserRequestDto getUserOrThrow(Long userId) {
        List<UserRequestDto> users = userClient.getUsersById(List.of(userId));
        if (users == null || users.isEmpty()) {
            throw new UserNotFoundException(userId);
        }
        return users.get(0);
    }
}