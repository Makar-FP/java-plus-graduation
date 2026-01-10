package ru.practicum.userservice.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.userservice.dto.UserCreateDto;
import ru.practicum.userservice.dto.UserRequestDto;
import ru.practicum.userservice.model.User;

@Component
public class UserMapper {

    public static User toEntity(UserCreateDto userCreateDto) {
        return User.builder()
                .email(userCreateDto.getEmail())
                .name(userCreateDto.getName())
                .build();
    }

    public static UserRequestDto toRequestDto(User user) {
        return UserRequestDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }
}
