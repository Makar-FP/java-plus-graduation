package ru.practicum.userservice.service;

import ru.practicum.userservice.dto.UserCreateDto;
import ru.practicum.userservice.dto.UserRequestDto;

import java.util.List;

public interface UserService {
    UserRequestDto create(UserCreateDto userCreateDto);

    List<UserRequestDto> get(List<Integer> ids, int from, int size);

    void delete(Long userId);
}
