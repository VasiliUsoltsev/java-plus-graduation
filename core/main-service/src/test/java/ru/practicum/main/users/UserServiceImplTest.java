package ru.practicum.main.users;

import ru.practicum.main.dto.request.NewUserRequest;
import ru.practicum.main.dto.request.UsersRequest;
import ru.practicum.main.dto.response.UserDto;
import ru.practicum.main.exception.model.ConflictException;
import ru.practicum.main.exception.model.NotFoundException;
import ru.practicum.main.model.User;
import ru.practicum.main.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import ru.practicum.main.service.impl.UserServiceImpl;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private NewUserRequest validRequest;
    private User user1;
    private User user2;
    private User user3;

    @BeforeEach
    void setUp() {
        validRequest = new NewUserRequest();
        validRequest.setName("John Doe");
        validRequest.setEmail("john@example.com");

        user1 = new User();
        user1.setId(1L);
        user1.setName("John Doe");
        user1.setEmail("john@example.com");

        user2 = new User();
        user2.setId(2L);
        user2.setName("Jane Smith");
        user2.setEmail("jane@example.com");

        user3 = new User();
        user3.setId(3L);
        user3.setName("Bob Johnson");
        user3.setEmail("bob@example.com");
    }

    @Test
    void createUser_ShouldReturnUserDto_WhenValidRequest() {
        when(userRepository.existsByEmail(validRequest.getEmail())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(user1);

        UserDto result = userService.createUser(validRequest);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("John Doe");
        assertThat(result.getEmail()).isEqualTo("john@example.com");

        verify(userRepository).existsByEmail(validRequest.getEmail());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_ShouldThrowEmailAlreadyExistsException_WhenEmailExists() {
        when(userRepository.existsByEmail(validRequest.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(validRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Пользователь с email 'john@example.com' уже существует");

        verify(userRepository).existsByEmail(validRequest.getEmail());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void findUsers_ShouldReturnAllUsers_WithPagination() {
        UsersRequest request = new UsersRequest(null, 0, 2);
        List<User> users = Arrays.asList(user1, user2);
        Page<User> userPage = new PageImpl<>(users);

        when(userRepository.findAllOrdered(any(Pageable.class))).thenReturn(userPage);

        List<UserDto> result = userService.findUsers(request);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getName()).isEqualTo("John Doe");
        assertThat(result.get(1).getId()).isEqualTo(2L);
        assertThat(result.get(1).getName()).isEqualTo("Jane Smith");

        verify(userRepository).findAllOrdered(any(Pageable.class));
        verify(userRepository, never()).findByIds(any(), any());
    }

    @Test
    void findUsers_ShouldReturnSecondPage() {
        UsersRequest request = new UsersRequest(null, 2, 2);
        List<User> users = Arrays.asList(user3);
        Page<User> userPage = new PageImpl<>(users);

        when(userRepository.findAllOrdered(any(Pageable.class))).thenReturn(userPage);

        List<UserDto> result = userService.findUsers(request);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(3L);
        assertThat(result.get(0).getName()).isEqualTo("Bob Johnson");
    }

    @Test
    void findUsers_ShouldReturnEmpty_WhenPageOutOfBounds() {
        UsersRequest request = new UsersRequest(null, 10, 20);
        Page<User> emptyPage = Page.empty();

        when(userRepository.findAllOrdered(any(Pageable.class))).thenReturn(emptyPage);

        List<UserDto> result = userService.findUsers(request);

        assertThat(result).isEmpty();
    }

    @Test
    void findUsers_ShouldReturnAllUsers_WhenFromAndSizeNull() {
        UsersRequest request = new UsersRequest();
        request.setIds(null);
        request.setFrom(null);
        request.setSize(null);

        List<User> users = Arrays.asList(user1, user2, user3);
        Page<User> userPage = new PageImpl<>(users);

        when(userRepository.findAllOrdered(any(Pageable.class))).thenReturn(userPage);

        List<UserDto> result = userService.findUsers(request);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(1).getId()).isEqualTo(2L);
        assertThat(result.get(2).getId()).isEqualTo(3L);
    }

    @Test
    void findUsers_ShouldFilterByIds() {
        List<Long> ids = Arrays.asList(1L, 3L);
        UsersRequest request = new UsersRequest(ids, 0, 20);
        List<User> users = Arrays.asList(user1, user3);
        Page<User> userPage = new PageImpl<>(users);

        when(userRepository.findByIds(eq(ids), any(Pageable.class))).thenReturn(userPage);

        List<UserDto> result = userService.findUsers(request);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(1).getId()).isEqualTo(3L);

        verify(userRepository).findByIds(eq(ids), any(Pageable.class));
        verify(userRepository, never()).findAllOrdered(any());
    }

    @Test
    void findUsers_ShouldReturnFilteredAndPaginatedUsers() {
        List<Long> ids = Arrays.asList(1L, 2L, 3L);
        UsersRequest request = new UsersRequest(ids, 1, 1);
        List<User> users = Arrays.asList(user2);
        Page<User> userPage = new PageImpl<>(users);

        when(userRepository.findByIds(eq(ids), any(Pageable.class))).thenReturn(userPage);

        List<UserDto> result = userService.findUsers(request);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(2L);
        assertThat(result.get(0).getName()).isEqualTo("Jane Smith");
    }

    @Test
    void findUsers_ShouldHandleEmptyIdsList() {
        UsersRequest request = new UsersRequest(Arrays.asList(), 0, 20);
        List<User> users = Arrays.asList(user1, user2, user3);
        Page<User> userPage = new PageImpl<>(users);

        when(userRepository.findAllOrdered(any(Pageable.class))).thenReturn(userPage);

        List<UserDto> result = userService.findUsers(request);

        assertThat(result).hasSize(3);
        verify(userRepository).findAllOrdered(any(Pageable.class));
        verify(userRepository, never()).findByIds(any(), any());
    }

    @Test
    void deleteUser_ShouldDelete_WhenUserExists() {
        when(userRepository.existsById(1L)).thenReturn(true);

        userService.deleteUser(1L);

        verify(userRepository).existsById(1L);
        verify(userRepository).deleteById(1L);
    }

    @Test
    void deleteUser_ShouldThrowUserNotFoundException_WhenUserDoesNotExist() {
        when(userRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> userService.deleteUser(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Пользователь с id: 999 не найден");

        verify(userRepository).existsById(999L);
        verify(userRepository, never()).deleteById(any());
    }
}