package ru.practicum.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class UserRequestDto {
    private Long id;
    private String name;
    private String email;
}
