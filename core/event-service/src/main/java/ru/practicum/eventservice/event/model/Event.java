package ru.practicum.eventservice.event.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import ru.practicum.eventservice.category.model.Category;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "events")
@Builder(toBuilder = true)
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "annotation", length = 2000)
    private String annotation;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "confirmed_requests", nullable = false)
    @ColumnDefault("0")
    private Integer confirmedRequests;

    @Column(name = "created_on", nullable = false)
    private LocalDateTime createdOn;

    @Column(name = "description", length = 7000)
    private String description;

    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    @Column(name = "initiator_id", nullable = false)
    private Long initiatorId;

    @Column(name = "location_lat")
    private Float lat;

    @Column(name = "location_lon")
    private Float lon;

    @Column(name = "paid", nullable = false)
    @ColumnDefault("false")
    private Boolean paid;

    @Column(name = "participant_limit")
    @ColumnDefault("0")
    private Integer participantLimit;

    @Column(name = "published")
    private LocalDateTime publishedOn;

    @Column(name = "request_moderation")
    @ColumnDefault("true")
    private Boolean requestModeration;

    @Enumerated(EnumType.STRING)
    @Column(name = "state")
    private EventState state;

    @Column(name = "title", nullable = false, length = 120)
    private String title;

    @Column(name = "views")
    @ColumnDefault("0")
    private Integer views;

    @PrePersist
    public void prePersist() {
        if (paid == null) {
            paid = false;
        }
        if (requestModeration == null) {
            requestModeration = true;
        }
        if (participantLimit == null) {
            participantLimit = 0;
        }
    }

}
