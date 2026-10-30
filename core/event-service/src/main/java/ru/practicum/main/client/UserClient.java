package ru.practicum.main.client;

import jakarta.validation.constraints.Positive;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.main.dto.client.UserShortDto;

import java.util.List;

@FeignClient(name = "user-service")
public interface UserClient {
    @GetMapping("/users/batchusershort")
    List<UserShortDto> findShortByIds(@RequestParam List<Long> userIds);

    @GetMapping("/users/{userId}/usershort")
    UserShortDto findShortById(@PathVariable @Positive Long userId);
}