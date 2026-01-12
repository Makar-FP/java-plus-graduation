package ru.practicum.eventservice.category.service;

import ru.practicum.eventservice.dto.category.CategoryCreateDto;
import ru.practicum.eventservice.dto.category.CategoryRequestDto;

import java.util.List;

public interface CategoryService {
    CategoryRequestDto create(CategoryCreateDto categoryCreateDto);

    CategoryRequestDto update(CategoryCreateDto categoryCreateDto, Long catId);

    void delete(Long catId);

    List<CategoryRequestDto> get(int from, int size);

    CategoryRequestDto getById(Long catId);

}
