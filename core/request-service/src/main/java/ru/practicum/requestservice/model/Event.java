package ru.practicum.requestservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Event {
    private Long id;

    private String annotation;

    private Long categoryId;

    private Integer confirmedRequests;

    private LocalDateTime createdOn;

    private String description;

    private LocalDateTime eventDate;

    private Long initiatorId;

    private Float lat;

    private Float lon;

    private Boolean paid;

    private Integer participantLimit;

    private LocalDateTime publishedOn;

    private Boolean requestModeration;

    private EventState state;

    private String title;

    private Double rating;
}
