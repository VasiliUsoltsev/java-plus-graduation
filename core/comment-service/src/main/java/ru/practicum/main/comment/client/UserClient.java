package ru.practicum.main.comment.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.main.comment.dto.client.UserDto;

@FeignClient(name = "user-service")
public interface UserClient {
    @GetMapping("/users/{userId}/exists")
    Boolean existsById(@PathVariable Long userId);

    @GetMapping("/users/{userId}")
    UserDto findUserById(@PathVariable Long userId);
}