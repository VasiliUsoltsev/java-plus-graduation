package ru.practicum.main.EventComment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.main.controller.AdminEventCommentController;
import ru.practicum.main.dto.request.EventCommentRequestStatus;
import ru.practicum.main.dto.request.GetEventCommentParamDto;
import ru.practicum.main.dto.response.EventCommentAdminDto;
import ru.practicum.main.dto.response.UserDto;
import ru.practicum.main.exception.model.ConflictException;
import ru.practicum.main.exception.model.NotFoundException;
import ru.practicum.main.model.enums.EventCommentStatus;
import ru.practicum.main.service.EventCommentService;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminEventCommentControllerTest {

    @Mock
    private EventCommentService eventCommentService;

    @InjectMocks
    private AdminEventCommentController controller;

    private UserDto userDto;
    private EventCommentAdminDto commentAdminDto;
    private List<EventCommentAdminDto> commentList;

    @BeforeEach
    void setUp() {
        userDto = UserDto.builder()
                .id(1L)
                .name("Test Author")
                .email("test@example.com")
                .build();

        commentAdminDto = EventCommentAdminDto.builder()
                .id(1L)
                .content("Test comment")
                .author(userDto)
                .event("Test Event")
                .status(EventCommentStatus.WAITING)
                .created(LocalDateTime.now())
                .build();

        commentList = List.of(commentAdminDto);
    }

    // ==================== GET /admin/comments/{commentId} ====================

    @Test
    @DisplayName("GET /admin/comments/{commentId} - должен вернуть комментарий по ID")
    void getCommentById_shouldReturnComment() {
        Long commentId = 1L;

        when(eventCommentService.getCommentById(commentId))
                .thenReturn(commentAdminDto);

        EventCommentAdminDto result = controller.getCommentById(commentId);

        assertNotNull(result);
        assertEquals(commentId, result.getId());
        assertEquals("Test comment", result.getContent());
        assertEquals(EventCommentStatus.WAITING, result.getStatus());
        assertNotNull(result.getAuthor());
        assertEquals(userDto, result.getAuthor());
        verify(eventCommentService, times(1)).getCommentById(commentId);
    }

    @Test
    @DisplayName("GET /admin/comments/{commentId} - должен пробросить исключение если комментарий не найден")
    void getCommentById_shouldThrowException_whenCommentNotFound() {
        Long commentId = 999L;

        when(eventCommentService.getCommentById(commentId))
                .thenThrow(new NotFoundException("Comment was not found with id=" + commentId));

        assertThrows(NotFoundException.class, () -> controller.getCommentById(commentId));
        verify(eventCommentService, times(1)).getCommentById(commentId);
    }

    // ==================== GET /admin/comments ====================

    @Test
    @DisplayName("GET /admin/comments - должен вернуть список всех комментариев с параметрами по умолчанию")
    void getComments_shouldReturnCommentsListWithDefaultParams() {
        when(eventCommentService.getAllComments(eq(EventCommentRequestStatus.ALL), any(GetEventCommentParamDto.class)))
                .thenReturn(commentList);

        List<EventCommentAdminDto> result = controller.getComments(EventCommentRequestStatus.ALL, 0, 10);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(commentList, result);
        verify(eventCommentService, times(1)).getAllComments(eq(EventCommentRequestStatus.ALL), any(GetEventCommentParamDto.class));
    }

    @Test
    @DisplayName("GET /admin/comments - должен вернуть список с фильтром по статусу WAITING")
    void getComments_shouldReturnCommentsListWithWaitingStatus() {
        EventCommentRequestStatus state = EventCommentRequestStatus.WAITING;

        EventCommentAdminDto waitingComment = EventCommentAdminDto.builder()
                .id(2L)
                .content("Waiting comment")
                .author(userDto)
                .event("Test Event")
                .status(EventCommentStatus.WAITING)
                .created(LocalDateTime.now())
                .build();

        List<EventCommentAdminDto> waitingList = List.of(waitingComment);

        when(eventCommentService.getAllComments(eq(state), any(GetEventCommentParamDto.class)))
                .thenReturn(waitingList);

        List<EventCommentAdminDto> result = controller.getComments(state, 0, 10);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(EventCommentStatus.WAITING, result.get(0).getStatus());
        verify(eventCommentService, times(1)).getAllComments(eq(state), any(GetEventCommentParamDto.class));
    }

    @Test
    @DisplayName("GET /admin/comments - должен вернуть список с фильтром по статусу APPROVED")
    void getComments_shouldReturnCommentsListWithApprovedStatus() {
        EventCommentRequestStatus state = EventCommentRequestStatus.APPROVED;


        EventCommentAdminDto approvedComment = EventCommentAdminDto.builder()
                .id(3L)
                .content("Approved comment")
                .author(userDto)
                .event("Test Event")
                .status(EventCommentStatus.APPROVED)
                .created(LocalDateTime.now())
                .build();

        List<EventCommentAdminDto> approvedList = List.of(approvedComment);

        when(eventCommentService.getAllComments(eq(state), any(GetEventCommentParamDto.class)))
                .thenReturn(approvedList);

        List<EventCommentAdminDto> result = controller.getComments(state, 0, 10);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(EventCommentStatus.APPROVED, result.get(0).getStatus());
        verify(eventCommentService, times(1)).getAllComments(eq(state), any(GetEventCommentParamDto.class));
    }

    @Test
    @DisplayName("GET /admin/comments - должен вернуть пустой список если комментариев нет")
    void getComments_shouldReturnEmptyList() {
        when(eventCommentService.getAllComments(eq(EventCommentRequestStatus.ALL), any(GetEventCommentParamDto.class)))
                .thenReturn(List.of());

        List<EventCommentAdminDto> result = controller.getComments(EventCommentRequestStatus.ALL, 0, 10);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(eventCommentService, times(1)).getAllComments(eq(EventCommentRequestStatus.ALL), any(GetEventCommentParamDto.class));
    }

    @Test
    @DisplayName("GET /admin/comments - должен передавать корректные параметры пагинации в сервис")
    void getComments_shouldPassCorrectPaginationParams() {
        int from = 5;
        int size = 20;

        controller.getComments(EventCommentRequestStatus.ALL, from, size);

        verify(eventCommentService, times(1)).getAllComments(eq(EventCommentRequestStatus.ALL), any(GetEventCommentParamDto.class));
    }

    // ==================== PATCH /admin/comments/{commentId}/approve ====================

    @Test
    @DisplayName("PATCH /admin/comments/{commentId}/approve - должен одобрить комментарий")
    void approve_shouldApproveComment() {
        Long commentId = 1L;

        EventCommentAdminDto approvedComment = EventCommentAdminDto.builder()
                .id(commentId)
                .content("Test comment")
                .author(userDto)
                .event("Test Event")
                .status(EventCommentStatus.APPROVED)
                .created(LocalDateTime.now())
                .build();

        when(eventCommentService.approve(commentId))
                .thenReturn(approvedComment);

        EventCommentAdminDto result = controller.approve(commentId);

        assertNotNull(result);
        assertEquals(commentId, result.getId());
        assertEquals(EventCommentStatus.APPROVED, result.getStatus());
        assertNotNull(result.getAuthor());
        assertEquals(userDto, result.getAuthor());
        verify(eventCommentService, times(1)).approve(commentId);
    }

    @Test
    @DisplayName("PATCH /admin/comments/{commentId}/approve - должен пробросить исключение если комментарий не найден")
    void approve_shouldThrowException_whenCommentNotFound() {
        Long commentId = 999L;

        when(eventCommentService.approve(commentId))
                .thenThrow(new NotFoundException("Comment was not found with id=" + commentId));

        assertThrows(NotFoundException.class, () -> controller.approve(commentId));
        verify(eventCommentService, times(1)).approve(commentId);
    }

    @Test
    @DisplayName("PATCH /admin/comments/{commentId}/approve - должен пробросить исключение если комментарий уже одобрен")
    void approve_shouldThrowException_whenCommentAlreadyApproved() {
        Long commentId = 1L;

        when(eventCommentService.approve(commentId))
                .thenThrow(new ConflictException("Comment is already approved"));

        assertThrows(ConflictException.class, () -> controller.approve(commentId));
        verify(eventCommentService, times(1)).approve(commentId);
    }

    // ==================== PATCH /admin/comments/{commentId}/reject ====================

    @Test
    @DisplayName("PATCH /admin/comments/{commentId}/reject - должен отклонить комментарий")
    void reject_shouldRejectComment() {
        Long commentId = 2L;

        EventCommentAdminDto rejectedComment = EventCommentAdminDto.builder()
                .id(commentId)
                .content("Test comment 2")
                .author(userDto)
                .event("Test Event")
                .status(EventCommentStatus.REJECTED)
                .created(LocalDateTime.now())
                .build();

        when(eventCommentService.reject(commentId))
                .thenReturn(rejectedComment);

        EventCommentAdminDto result = controller.reject(commentId);

        assertNotNull(result);
        assertEquals(commentId, result.getId());
        assertEquals(EventCommentStatus.REJECTED, result.getStatus());
        assertNotNull(result.getAuthor());
        verify(eventCommentService, times(1)).reject(commentId);
    }

    @Test
    @DisplayName("PATCH /admin/comments/{commentId}/reject - должен пробросить исключение если комментарий не найден")
    void reject_shouldThrowException_whenCommentNotFound() {
        Long commentId = 999L;

        when(eventCommentService.reject(commentId))
                .thenThrow(new NotFoundException("Comment was not found with id=" + commentId));

        assertThrows(NotFoundException.class, () -> controller.reject(commentId));
        verify(eventCommentService, times(1)).reject(commentId);
    }

    @Test
    @DisplayName("PATCH /admin/comments/{commentId}/reject - должен пробросить исключение если комментарий уже отклонен")
    void reject_shouldThrowException_whenCommentAlreadyRejected() {
        Long commentId = 2L;

        when(eventCommentService.reject(commentId))
                .thenThrow(new ConflictException("Comment is already rejected"));

        assertThrows(ConflictException.class, () -> controller.reject(commentId));
        verify(eventCommentService, times(1)).reject(commentId);
    }
}