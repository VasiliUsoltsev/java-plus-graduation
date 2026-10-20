package ru.practicum.main.controller;

import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import ru.practicum.main.dto.request.NewUserRequest;
import ru.practicum.main.dto.request.UsersRequest;
import ru.practicum.main.dto.response.UserDto;
import ru.practicum.main.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@Validated
public class AdminUserController {

    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto createUser(@Valid @RequestBody NewUserRequest request) {
        return userService.createUser(request);
    }

    @GetMapping
    public List<UserDto> getUsers(@RequestParam(required = false) List<Long> ids,
                                  @RequestParam(defaultValue = "0") @Min(0) Integer from,
                                  @RequestParam(defaultValue = "10") @Min(1) Integer size) {

        UsersRequest request = UsersRequest.builder()
                .ids(ids)
                .from(from)
                .size(size)
                .build();

        return userService.findUsers(request);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
    }
}