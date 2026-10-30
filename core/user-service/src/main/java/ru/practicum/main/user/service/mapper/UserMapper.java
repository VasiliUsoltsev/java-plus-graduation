package ru.practicum.main.user.service.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.practicum.main.dto.response.UserDto;
import ru.practicum.main.user.dto.response.UserShortDto;
import ru.practicum.main.user.model.User;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UserMapper {

    public static UserDto toDto(User user) {
        return new UserDto(
                user.getId(),
                user.getName(),
                user.getEmail()
        );
    }

    public static UserShortDto toShortDto(User user) {
        return new UserShortDto(
                user.getId(),
                user.getName()
        );
    }
}
