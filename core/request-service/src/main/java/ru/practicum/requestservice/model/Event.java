package ru.practicum.requestservice.model;

import lombok.*;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Event {

    private Long id;

    private String annotation;

    private Category category;

    private Integer confirmedRequests;

    private LocalDateTime createdOn;

    private String description;

    private LocalDateTime eventDate;

    private Long initiatorId;

    public Float lat;

    public Float lon;

    private Boolean paid;

    private Integer participantLimit;

    private LocalDateTime publishedOn;

    private Boolean requestModeration = true;

    private EventState state;

    private String title;

    private Integer views;
}
