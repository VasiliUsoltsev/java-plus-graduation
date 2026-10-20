package ru.practicum.main.users;

import com.fasterxml.jackson.databind.ObjectMapper;
import ru.practicum.main.controller.AdminUserController;
import ru.practicum.main.dto.request.NewUserRequest;
import ru.practicum.main.dto.request.UsersRequest;
import ru.practicum.main.dto.response.UserDto;
import ru.practicum.main.exception.model.ConflictException;
import ru.practicum.main.exception.model.NotFoundException;
import ru.practicum.main.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminUserController.class)
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @Test
    void createUser_ShouldReturnCreated_WhenValidRequest() throws Exception {
        NewUserRequest request = new NewUserRequest();
        request.setName("John Doe");
        request.setEmail("john@example.com");

        UserDto response = new UserDto(1L, "John Doe", "john@example.com");

        when(userService.createUser(any(NewUserRequest.class))).thenReturn(response);

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"));
    }

    @Test
    void createUser_ShouldReturnBadRequest_WhenNameIsEmpty() throws Exception {
        NewUserRequest request = new NewUserRequest();
        request.setName("");
        request.setEmail("john@example.com");

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUser_ShouldReturnBadRequest_WhenNameTooShort() throws Exception {
        NewUserRequest request = new NewUserRequest();
        request.setName("A");
        request.setEmail("john@example.com");

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUser_ShouldReturnBadRequest_WhenNameTooLong() throws Exception {
        NewUserRequest request = new NewUserRequest();
        request.setName("A".repeat(251));
        request.setEmail("john@example.com");

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUser_ShouldReturnBadRequest_WhenEmailIsEmpty() throws Exception {
        NewUserRequest request = new NewUserRequest();
        request.setName("John Doe");
        request.setEmail("");

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUser_ShouldReturnBadRequest_WhenEmailInvalid() throws Exception {
        NewUserRequest request = new NewUserRequest();
        request.setName("John Doe");
        request.setEmail("invalid-email");

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUser_ShouldReturnConflict_WhenEmailAlreadyExists() throws Exception {
        NewUserRequest request = new NewUserRequest();
        request.setName("John Doe");
        request.setEmail("existing@example.com");

        when(userService.createUser(any(NewUserRequest.class)))
                .thenThrow(new ConflictException("Пользователь с email 'existing@example.com' уже существует"));

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("For the requested operation the conditions are not met."))
                .andExpect(jsonPath("$.message").value("Пользователь с email 'existing@example.com' уже существует"));
    }

    // ==================== ТЕСТЫ ДЛЯ GET /admin/users ====================

    @Test
    void getUsers_ShouldReturnUsers_WhenValidParams() throws Exception {
        List<UserDto> users = Arrays.asList(
                new UserDto(1L, "John Doe", "john@example.com"),
                new UserDto(2L, "Jane Smith", "jane@example.com")
        );

        when(userService.findUsers(any(UsersRequest.class))).thenReturn(users);

        mockMvc.perform(get("/admin/users")
                        .param("from", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("John Doe"))
                .andExpect(jsonPath("$[0].email").value("john@example.com"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].name").value("Jane Smith"))
                .andExpect(jsonPath("$[1].email").value("jane@example.com"));
    }

    @Test
    void getUsers_ShouldReturnFilteredUsers_WhenIdsProvided() throws Exception {
        List<UserDto> users = Arrays.asList(
                new UserDto(1L, "John Doe", "john@example.com"),
                new UserDto(3L, "Bob Johnson", "bob@example.com")
        );

        when(userService.findUsers(any(UsersRequest.class))).thenReturn(users);

        mockMvc.perform(get("/admin/users")
                        .param("ids", "1", "3")
                        .param("from", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(3));
    }

    @Test
    void getUsers_ShouldUseDefaultValues_WhenParamsNotProvided() throws Exception {
        List<UserDto> users = Arrays.asList(new UserDto(1L, "John Doe", "john@example.com"));

        when(userService.findUsers(any(UsersRequest.class))).thenReturn(users);

        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void getUsers_ShouldReturnBadRequest_WhenFromNegative() throws Exception {
        mockMvc.perform(get("/admin/users")
                        .param("from", "-1")
                        .param("size", "20"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUsers_ShouldReturnBadRequest_WhenSizeZero() throws Exception {
        mockMvc.perform(get("/admin/users")
                        .param("from", "0")
                        .param("size", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUsers_ShouldReturnBadRequest_WhenSizeNegative() throws Exception {
        mockMvc.perform(get("/admin/users")
                        .param("from", "0")
                        .param("size", "-5"))
                .andExpect(status().isBadRequest());
    }

    // ==================== ТЕСТЫ ДЛЯ DELETE /admin/users/{userId} ====================

    @Test
    void deleteUser_ShouldReturnNoContent_WhenUserExists() throws Exception {
        mockMvc.perform(delete("/admin/users/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteUser_ShouldReturnNotFound_WhenUserDoesNotExist() throws Exception {
        doThrow(new NotFoundException("Пользователь с id: 999 не найден"))
                .when(userService).deleteUser(999L);

        mockMvc.perform(delete("/admin/users/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("The required object was not found."))
                .andExpect(jsonPath("$.message").value("Пользователь с id: 999 не найден"));
    }

    @Test
    void deleteUser_ShouldReturnBadRequest_WhenInvalidUserId() throws Exception {
        mockMvc.perform(delete("/admin/users/abc"))
                .andExpect(status().isBadRequest());
    }
}