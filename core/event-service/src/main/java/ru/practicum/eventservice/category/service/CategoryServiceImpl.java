package ru.practicum.eventservice.category.service;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.practicum.eventservice.dto.category.CategoryCreateDto;
import ru.practicum.eventservice.dto.category.CategoryRequestDto;
import ru.practicum.eventservice.category.mapper.CategoryMapper;
import ru.practicum.eventservice.category.model.Category;
import ru.practicum.eventservice.category.repo.CategoryRepository;
import ru.practicum.eventservice.event.repo.EventRepository;
import ru.practicum.eventservice.exception.CategoryNotFoundException;
import ru.practicum.eventservice.exception.ConflictException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;

    @Override
    public CategoryRequestDto create(CategoryCreateDto categoryCreateDto) {
        return CategoryMapper.toRequestDto(
                categoryRepository.save(
                        CategoryMapper.toEntity(categoryCreateDto)
                )
        );
    }

    @Override
    public CategoryRequestDto update(CategoryCreateDto categoryCreateDto, Long catId) {
        Category category = categoryRepository.findById(catId)
                .orElseThrow(() -> new CategoryNotFoundException(catId));

        category.setName(categoryCreateDto.getName());

        return CategoryMapper.toRequestDto(
                categoryRepository.save(category)
        );
    }

    @Override
    public void delete(Long catId) {
        categoryRepository.findById(catId)
                .orElseThrow(() -> new CategoryNotFoundException(catId));

        if (eventRepository.findFirstByCategoryId(catId).isPresent()) {
                throw  new ConflictException("The category is not empty");
        }

        categoryRepository.deleteById(catId);
    }

    @Override
    public List<CategoryRequestDto> get(int from, int size) {
        Pageable pageable = PageRequest.of(from / size, size);

        Page<Category> page = categoryRepository.findAll(pageable);

        return page.stream()
                .map(CategoryMapper::toRequestDto)
                .collect(Collectors.toList());
    }

    @Override
    public CategoryRequestDto getById(Long catId) {
        Category category = categoryRepository.findById(catId)
                .orElseThrow(() -> new CategoryNotFoundException(catId));

        return CategoryMapper.toRequestDto(
                categoryRepository.save(category)
        );
    }
}
