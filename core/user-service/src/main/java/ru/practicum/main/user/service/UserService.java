package ru.practicum.main.user.service;

import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.main.dto.response.UserDto;
import ru.practicum.main.user.dto.request.NewUserRequest;
import ru.practicum.main.user.dto.request.UsersRequest;
import ru.practicum.main.user.dto.response.UserShortDto;
import ru.practicum.main.user.model.User;

import java.util.List;

public interface UserService {
    UserDto createUser(NewUserRequest userRequest);

    List<UserDto> findUsers(UsersRequest usersRequest);

    void deleteUser(Long id);

    Boolean existsById(Long userId);

    User findUserById(Long userId);

    List<UserShortDto> findUserShortByIds(List<Long> userIds);

    UserShortDto findUserShortById(Long userId);
}
