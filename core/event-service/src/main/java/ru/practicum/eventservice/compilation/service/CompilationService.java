package ru.practicum.eventservice.compilation.service;

import ru.practicum.eventservice.dto.compilation.CompilationCreateDto;
import ru.practicum.eventservice.dto.compilation.CompilationRequestDto;
import ru.practicum.eventservice.dto.compilation.CompilationUpdateDto;

import java.util.List;

public interface CompilationService {

    CompilationRequestDto create(CompilationCreateDto compilationCreateDto);

    CompilationRequestDto update(CompilationUpdateDto compilationUpdateDto, Long compId);

    void delete(Long compId);

    List<CompilationRequestDto> get(Boolean pinned, int from, int size);

    CompilationRequestDto getById(Long compId);
}
