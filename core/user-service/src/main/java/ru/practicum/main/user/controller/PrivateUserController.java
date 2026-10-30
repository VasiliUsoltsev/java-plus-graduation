package ru.practicum.main.user.controller;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.main.user.dto.response.UserShortDto;
import ru.practicum.main.user.model.User;
import ru.practicum.main.user.service.UserService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Validated
public class PrivateUserController {

    private final UserService userService;

    @GetMapping("/{userId}/exists")
    public Boolean existsById(@Positive @PathVariable Long userId) {
        return userService.existsById(userId);
    }

    @GetMapping("/{userId}")
    public User findById(@Positive @PathVariable Long userId) {
        return userService.findUserById(userId);
    }

    @GetMapping("/batchusershort")
    public List<UserShortDto> findShortByIds(@RequestParam List<Long> userIds) {
        return userService.findUserShortByIds(userIds);
    }

    @GetMapping("/authornamebatch")
    public List<Object[]> getAuthorNameBatch(@RequestParam List<Long> authorIds) {
        return userService.getAuthorNameBatch(authorIds);
    }

    @GetMapping("/{userId}/usershort")
    public UserShortDto findShortById(@Positive @PathVariable Long userId) {
        return userService.findUserShortById(userId);
    }
}