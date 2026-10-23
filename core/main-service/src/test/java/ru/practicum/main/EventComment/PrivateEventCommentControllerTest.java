package ru.practicum.main.EventComment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.main.controller.PrivateEventCommentController;
import ru.practicum.main.dto.request.*;
import ru.practicum.main.dto.response.EventCommentAuthorDto;
import ru.practicum.main.dto.response.EventCommentUserDto;
import ru.practicum.main.model.enums.EventCommentStatus;
import ru.practicum.main.service.EventCommentService;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrivateEventCommentControllerTest {

    @Mock
    private EventCommentService eventCommentService;

    @InjectMocks
    private PrivateEventCommentController controller;

    private EventCommentDto newCommentDto;
    private EventCommentDto updateCommentDto;
    private EventCommentAuthorDto commentAuthorDto;
    private EventCommentUserDto commentUserDto;
    private List<EventCommentAuthorDto> commentAuthorList;
    private List<EventCommentUserDto> commentUserList;
    private Long eventId;
    private Long userId;
    private Long commentId;

    @BeforeEach
    void setUp() {
        eventId = 1L;
        userId = 1L;
        commentId = 1L;

        newCommentDto = EventCommentDto.builder()
                .content("Test comment")
                .build();

        updateCommentDto = EventCommentDto.builder()
                .content("Updated comment")
                .build();

        commentAuthorDto = EventCommentAuthorDto.builder()
                .id(commentId)
                .content("Test comment")
                .author("Test Author")
                .event("Test Event")
                .status(EventCommentStatus.WAITING)
                .created(LocalDateTime.now())
                .build();

        // ✅ Правильно: без поля event
        commentUserDto = EventCommentUserDto.builder()
                .id(commentId)
                .content("Test comment")
                .author("Test Author")
                .created(LocalDateTime.now())
                .build();

        commentAuthorList = List.of(commentAuthorDto);
        commentUserList = List.of(commentUserDto);
    }

    // ==================== POST /events/{eventId}/comments ====================

    @Test
    @DisplayName("POST /events/{eventId}/comments - должен успешно создать комментарий")
    void addComment_shouldCreateComment() {
        when(eventCommentService.addComment(any(EventCommentDto.class), any(NewEventCommentParamDto.class)))
                .thenReturn(commentAuthorDto);

        EventCommentAuthorDto result = controller.addComment(newCommentDto, eventId, userId);

        assertNotNull(result);
        assertEquals(commentAuthorDto.getId(), result.getId());
        assertEquals(commentAuthorDto.getContent(), result.getContent());
        assertEquals(commentAuthorDto.getStatus(), result.getStatus());
        verify(eventCommentService, times(1)).addComment(any(EventCommentDto.class), any(NewEventCommentParamDto.class));
    }

    @Test
    @DisplayName("POST /events/{eventId}/comments - должен передавать корректные параметры в сервис")
    void addComment_shouldPassCorrectParamsToService() {
        controller.addComment(newCommentDto, eventId, userId);

        verify(eventCommentService, times(1)).addComment(
                eq(newCommentDto),
                argThat(param ->
                        param.getEventId().equals(eventId) &&
                                param.getUserId().equals(userId)
                )
        );
    }

    // ==================== PATCH /events/{eventId}/comments/{commentId} ====================

    @Test
    @DisplayName("PATCH /events/{eventId}/comments/{commentId} - должен успешно обновить комментарий")
    void updateComment_shouldUpdateComment() {
        when(eventCommentService.updateComment(any(EventCommentDto.class), any(EventCommentParamDto.class)))
                .thenReturn(commentAuthorDto);

        EventCommentAuthorDto result = controller.updateComment(updateCommentDto, eventId, commentId, userId);

        assertNotNull(result);
        assertEquals(commentAuthorDto.getId(), result.getId());
        verify(eventCommentService, times(1)).updateComment(any(EventCommentDto.class), any(EventCommentParamDto.class));
    }

    @Test
    @DisplayName("PATCH /events/{eventId}/comments/{commentId} - должен передавать корректные параметры в сервис")
    void updateComment_shouldPassCorrectParamsToService() {
        controller.updateComment(updateCommentDto, eventId, commentId, userId);

        verify(eventCommentService, times(1)).updateComment(
                eq(updateCommentDto),
                argThat(param ->
                        param.getEventId().equals(eventId) &&
                                param.getUserId().equals(userId) &&
                                param.getCommentId().equals(commentId)
                )
        );
    }

    // ==================== DELETE /events/{eventId}/comments/{commentId} ====================

    @Test
    @DisplayName("DELETE /events/{eventId}/comments/{commentId} - должен успешно удалить комментарий")
    void deleteComment_shouldDeleteComment() {
        doNothing().when(eventCommentService).deleteComment(any(EventCommentParamDto.class));

        controller.deleteComment(commentId, eventId, userId);

        verify(eventCommentService, times(1)).deleteComment(any(EventCommentParamDto.class));
    }

    @Test
    @DisplayName("DELETE /events/{eventId}/comments/{commentId} - должен передавать корректные параметры в сервис")
    void deleteComment_shouldPassCorrectParamsToService() {
        controller.deleteComment(commentId, eventId, userId);

        verify(eventCommentService, times(1)).deleteComment(
                argThat(param ->
                        param.getEventId().equals(eventId) &&
                                param.getUserId().equals(userId) &&
                                param.getCommentId().equals(commentId)
                )
        );
    }

    // ==================== GET /events/{eventId}/comments ====================

    @Test
    @DisplayName("GET /events/{eventId}/comments - должен вернуть список комментариев пользователя")
    void getCommentsEventByUser_shouldReturnCommentsList() {
        when(eventCommentService.getCommentsEventByUser(any(EventCommentParamDto.class)))
                .thenReturn(commentAuthorList);

        List<EventCommentAuthorDto> result = controller.getCommentsEventByUser(eventId, userId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(commentAuthorList, result);
        verify(eventCommentService, times(1)).getCommentsEventByUser(any(EventCommentParamDto.class));
    }

    @Test
    @DisplayName("GET /events/{eventId}/comments - должен вернуть пустой список если комментариев нет")
    void getCommentsEventByUser_shouldReturnEmptyList() {
        when(eventCommentService.getCommentsEventByUser(any(EventCommentParamDto.class)))
                .thenReturn(List.of());

        List<EventCommentAuthorDto> result = controller.getCommentsEventByUser(eventId, userId);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(eventCommentService, times(1)).getCommentsEventByUser(any(EventCommentParamDto.class));
    }

    @Test
    @DisplayName("GET /events/{eventId}/comments - должен передавать корректные параметры в сервис")
    void getCommentsEventByUser_shouldPassCorrectParamsToService() {
        controller.getCommentsEventByUser(eventId, userId);

        verify(eventCommentService, times(1)).getCommentsEventByUser(
                argThat(param ->
                        param.getEventId().equals(eventId) &&
                                param.getUserId().equals(userId)
                )
        );
    }

    // ==================== GET /events/{eventId}/comments/approved ====================

    @Test
    @DisplayName("GET /events/{eventId}/comments/approved - должен вернуть одобренные комментарии события")
    void getCommentsEventApproved_shouldReturnApprovedComments() {
        when(eventCommentService.getCommentsByEvent(anyLong(), any(GetEventCommentParamDto.class)))
                .thenReturn(commentUserList);

        List<EventCommentUserDto> result = controller.getCommentsEventApproved(eventId, 0, 10);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(commentUserDto.getContent(), result.get(0).getContent());
        verify(eventCommentService, times(1)).getCommentsByEvent(anyLong(), any(GetEventCommentParamDto.class));
    }

    @Test
    @DisplayName("GET /events/{eventId}/comments/approved - должен вернуть пустой список если одобренных комментариев нет")
    void getCommentsEventApproved_shouldReturnEmptyList() {
        when(eventCommentService.getCommentsByEvent(anyLong(), any(GetEventCommentParamDto.class)))
                .thenReturn(List.of());

        List<EventCommentUserDto> result = controller.getCommentsEventApproved(eventId, 0, 10);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(eventCommentService, times(1)).getCommentsByEvent(anyLong(), any(GetEventCommentParamDto.class));
    }

    @Test
    @DisplayName("GET /events/{eventId}/comments/approved - должен передавать корректные параметры пагинации в сервис")
    void getCommentsEventApproved_shouldPassCorrectPaginationParamsToService() {
        int from = 5;
        int size = 20;

        controller.getCommentsEventApproved(eventId, from, size);

        verify(eventCommentService, times(1)).getCommentsByEvent(
                eq(eventId),
                argThat(param ->
                        param.getFrom().equals(from) &&
                                param.getSize().equals(size)
                )
        );
    }

    // ==================== ПРОВЕРКА АННОТАЦИЙ ====================

    @Test
    @DisplayName("Проверка наличия @PostMapping на методе addComment")
    void addComment_shouldHavePostMapping() throws NoSuchMethodException {
        Method method = PrivateEventCommentController.class
                .getMethod("addComment", EventCommentDto.class, Long.class, Long.class);
        PostMapping annotation = method.getAnnotation(PostMapping.class);
        assertNotNull(annotation, "Метод должен иметь аннотацию @PostMapping");
    }

    @Test
    @DisplayName("Проверка наличия @ResponseStatus(CREATED) на методе addComment")
    void addComment_shouldHaveResponseStatusCreated() throws NoSuchMethodException {
        Method method = PrivateEventCommentController.class
                .getMethod("addComment", EventCommentDto.class, Long.class, Long.class);
        ResponseStatus annotation = method.getAnnotation(ResponseStatus.class);
        assertNotNull(annotation);
        assertEquals(HttpStatus.CREATED, annotation.value());
    }

    @Test
    @DisplayName("Проверка наличия @PatchMapping на методе updateComment")
    void updateComment_shouldHavePatchMapping() throws NoSuchMethodException {
        Method method = PrivateEventCommentController.class
                .getMethod("updateComment", EventCommentDto.class, Long.class, Long.class, Long.class);
        PatchMapping annotation = method.getAnnotation(PatchMapping.class);
        assertNotNull(annotation, "Метод должен иметь аннотацию @PatchMapping");
    }

    @Test
    @DisplayName("Проверка наличия @DeleteMapping на методе deleteComment")
    void deleteComment_shouldHaveDeleteMapping() throws NoSuchMethodException {
        Method method = PrivateEventCommentController.class
                .getMethod("deleteComment", Long.class, Long.class, Long.class);
        DeleteMapping annotation = method.getAnnotation(DeleteMapping.class);
        assertNotNull(annotation, "Метод должен иметь аннотацию @DeleteMapping");
    }

    @Test
    @DisplayName("Проверка наличия @ResponseStatus(NO_CONTENT) на методе deleteComment")
    void deleteComment_shouldHaveResponseStatusNoContent() throws NoSuchMethodException {
        Method method = PrivateEventCommentController.class
                .getMethod("deleteComment", Long.class, Long.class, Long.class);
        ResponseStatus annotation = method.getAnnotation(ResponseStatus.class);
        assertNotNull(annotation);
        assertEquals(HttpStatus.NO_CONTENT, annotation.value());
    }

    @Test
    @DisplayName("Проверка наличия @GetMapping на методе getCommentsEventByUser")
    void getCommentsEventByUser_shouldHaveGetMapping() throws NoSuchMethodException {
        Method method = PrivateEventCommentController.class
                .getMethod("getCommentsEventByUser", Long.class, Long.class);
        GetMapping annotation = method.getAnnotation(GetMapping.class);
        assertNotNull(annotation, "Метод должен иметь аннотацию @GetMapping");
    }

    @Test
    @DisplayName("Проверка наличия @GetMapping на методе getCommentsEventApproved")
    void getCommentsEventApproved_shouldHaveGetMapping() throws NoSuchMethodException {
        Method method = PrivateEventCommentController.class
                .getMethod("getCommentsEventApproved", Long.class, Integer.class, Integer.class);
        GetMapping annotation = method.getAnnotation(GetMapping.class);
        assertNotNull(annotation, "Метод должен иметь аннотацию @GetMapping");
        assertEquals("/approved", annotation.value()[0]);
    }

    @Test
    @DisplayName("Проверка наличия @RequestMapping на классе")
    void controller_shouldHaveRequestMapping() {
        RequestMapping annotation = PrivateEventCommentController.class
                .getAnnotation(RequestMapping.class);
        assertNotNull(annotation);
        assertEquals("events/{eventId}/comments", annotation.value()[0]);
    }
}