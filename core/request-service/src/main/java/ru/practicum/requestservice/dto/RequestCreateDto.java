package ru.practicum.requestservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import ru.practicum.requestservice.model.RequestStatus;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class RequestCreateDto {
    private LocalDateTime created;

    private Long requesterId;

    private Long eventId;

    private RequestStatus status;
}
