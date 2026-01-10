package ru.practicum.eventservice.compilation.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.eventservice.compilation.model.Compilation;
import ru.practicum.eventservice.dto.compilation.CompilationCreateDto;
import ru.practicum.eventservice.dto.compilation.CompilationRequestDto;
import ru.practicum.eventservice.event.model.Event;

import java.util.List;

@Component
public class CompilationMapper {
    public static Compilation toEntity(CompilationCreateDto compilationCreateDto, List<Event> events) {
        Compilation compilation = new Compilation();
        compilation.setTitle(compilationCreateDto.getTitle());
        compilation.setPinned(compilationCreateDto.getPinned());
        compilation.setEvents(events);

        return compilation;
    }

    public static CompilationRequestDto toRequestDto(Compilation compilation) {
        return new CompilationRequestDto(
                compilation.getId(),
                compilation.getTitle(),
                compilation.getPinned(),
                compilation.getEvents()
        );
    }
}