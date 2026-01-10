package ru.practicum.eventservice.dto.category;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CategoryRequestDto {
    private Long id;
    private String name;
}
