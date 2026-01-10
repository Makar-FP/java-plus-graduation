package ru.practicum.eventservice.dto.compilation;

import lombok.AllArgsConstructor;
import lombok.Data;
import ru.practicum.eventservice.event.model.Event;

import java.util.List;

@Data
@AllArgsConstructor
public class CompilationRequestDto {
    private Long id;

    private String title;

    private Boolean pinned;

    private List<Event> events;
}
