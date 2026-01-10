package ru.practicum.requestservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.requestservice.dto.UserRequestDto;

import java.util.List;

@FeignClient(name = "user-service", path = "/admin/users")
public interface UserClient {

    @GetMapping()
    List<UserRequestDto> getUsersById(@RequestParam List<Long> ids);
}
