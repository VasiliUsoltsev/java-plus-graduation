package ru.practicum.main.user.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.dto.response.UserDto;
import ru.practicum.main.user.dto.request.NewUserRequest;
import ru.practicum.main.user.dto.request.UsersRequest;
import ru.practicum.main.user.dto.response.UserShortDto;
import ru.practicum.main.user.exception.model.ConflictException;
import ru.practicum.main.user.exception.model.NotFoundException;
import ru.practicum.main.user.model.User;
import ru.practicum.main.user.repository.UserRepository;
import ru.practicum.main.user.service.UserService;
import ru.practicum.main.user.service.mapper.UserMapper;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserDto createUser(NewUserRequest userRequest) {
        if (userRepository.existsByEmail(userRequest.getEmail())) {
            throw new ConflictException(
                    "Пользователь с email '" + userRequest.getEmail() + "' уже существует"
            );
        }

        User user = new User();
        user.setName(userRequest.getName());
        user.setEmail(userRequest.getEmail());

        User savedUser = userRepository.save(user);
        return UserMapper.toDto(savedUser);
    }

    @Override
    public List<UserDto> findUsers(UsersRequest request) {
        int from = request.getFrom() != null ? request.getFrom() : 0;
        int size = request.getSize() != null && request.getSize() > 0 ? request.getSize() : 10;

        int pageNumber = from / size;
        Pageable pageable = PageRequest.of(pageNumber, size);

        Page<User> userPage;


        if (request.getIds() != null && !request.getIds().isEmpty()) {
            userPage = userRepository.findByIds(request.getIds(), pageable);
        } else {
            userPage = userRepository.findAllOrdered(pageable);
        }


        return userPage.getContent().stream()
                .map(UserMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь с id: " + userId + " не найден");
        }
        userRepository.deleteById(userId);
    }

    @Override
    public Boolean existsById(Long userId) {
        return userRepository.existsById(userId);
    }

    @Override
    public User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id: " + userId + " не найден"));
    }

    @Override
    public List<UserShortDto> findUserShortByIds(List<Long> userIds) {
        List<User> list = userRepository.findByIds(userIds, Pageable.unpaged()).toList();
        return list.stream()
                .map(UserMapper::toShortDto)
                .toList();
    }

    @Override
    public UserShortDto findUserShortById(Long userId) {
        return UserMapper.toShortDto(findUserById(userId));
    }
}