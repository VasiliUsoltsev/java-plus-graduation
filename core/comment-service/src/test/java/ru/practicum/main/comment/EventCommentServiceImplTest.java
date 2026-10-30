package ru.practicum.main.comment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import ru.practicum.main.comment.client.EventClient;
import ru.practicum.main.comment.client.UserClient;
import ru.practicum.main.comment.dto.client.EventDto;
import ru.practicum.main.comment.dto.client.UserDto;
import ru.practicum.main.comment.dto.request.*;
import ru.practicum.main.comment.dto.response.EventCommentAdminDto;
import ru.practicum.main.comment.dto.response.EventCommentAuthorDto;
import ru.practicum.main.comment.dto.response.EventCommentUserDto;
import ru.practicum.main.comment.exception.model.AccessDeniedException;
import ru.practicum.main.comment.exception.model.ConflictException;
import ru.practicum.main.comment.exception.model.NotFoundException;
import ru.practicum.main.comment.model.EventComment;
import ru.practicum.main.comment.model.enums.EventCommentStatus;
import ru.practicum.main.comment.repository.EventCommentRepository;
import ru.practicum.main.comment.service.impl.EventCommentServiceImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventCommentServiceImplTest {

    @Mock
    private EventCommentRepository eventCommentRepository;

    @Mock
    private EventClient eventClient;

    @Mock
    private UserClient userClient;

    @InjectMocks
    private EventCommentServiceImpl service;

    private UserDto user;
    private EventDto event;
    private EventComment comment;
    private EventComment approvedComment;
    private EventComment rejectedComment;
    private EventCommentDto newCommentDto;
    private NewEventCommentParamDto newCommentParam;
    private EventCommentDto updateDto;
    private EventCommentParamDto paramDto;
    private GetEventCommentParamDto getParamDto;

    @BeforeEach
    void setUp() {
        user = UserDto.builder()
                .id(1L)
                .name("Test User")
                .build();

        event = EventDto.builder()
                .id(1L)
                .title("Test Event")
                .annotation("Test annotation")
                .eventDate(LocalDateTime.now().plusDays(5))
                .createdOn(LocalDateTime.now())
                .paid(false)
                .participantLimit(0)
                .requestModeration(true)
                .build();

        comment = EventComment.builder()
                .id(1L)
                .content("Test comment")
                .author(1L)
                .event(1L)
                .status(EventCommentStatus.WAITING)
                .created(LocalDateTime.now())
                .build();

        approvedComment = EventComment.builder()
                .id(2L)
                .content("Approved comment")
                .author(1L)
                .event(1L)
                .status(EventCommentStatus.APPROVED)
                .created(LocalDateTime.now())
                .build();

        rejectedComment = EventComment.builder()
                .id(3L)
                .content("Rejected comment")
                .author(1L)
                .event(1L)
                .status(EventCommentStatus.REJECTED)
                .created(LocalDateTime.now())
                .build();

        newCommentDto = EventCommentDto.builder()
                .content("New comment")
                .build();

        newCommentParam = NewEventCommentParamDto.builder()
                .eventId(1L)
                .userId(1L)
                .build();

        updateDto = EventCommentDto.builder()
                .content("Updated comment")
                .build();

        paramDto = EventCommentParamDto.builder()
                .eventId(1L)
                .userId(1L)
                .commentId(1L)
                .build();

        getParamDto = GetEventCommentParamDto.builder()
                .from(0)
                .size(10)
                .build();
    }

    // ==================== addComment ====================

    @Test
    @DisplayName("addComment - должен успешно создать комментарий")
    void addComment_shouldCreateComment() {
        when(eventClient.existsById(1L)).thenReturn(true);
        when(userClient.existsById(1L)).thenReturn(true);
        when(eventCommentRepository.save(any(EventComment.class))).thenReturn(comment);
        when(eventClient.findById(1L)).thenReturn(event);
        when(userClient.findUserById(1L)).thenReturn(user);

        EventCommentAuthorDto result = service.addComment(newCommentDto, newCommentParam);

        assertNotNull(result);
        assertEquals(comment.getId(), result.getId());
        assertEquals(comment.getContent(), result.getContent());
        assertEquals(EventCommentStatus.WAITING, result.getStatus());
        verify(eventClient, times(1)).existsById(1L);
        verify(userClient, times(1)).existsById(1L);
        verify(eventCommentRepository, times(1)).save(any(EventComment.class));
        verify(eventClient, times(1)).findById(1L);
        verify(userClient, times(1)).findUserById(1L);
    }

    @Test
    @DisplayName("addComment - должен выбросить NotFoundException если событие не найдено")
    void addComment_shouldThrowNotFoundException_whenEventNotFound() {
        when(eventClient.existsById(1L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> service.addComment(newCommentDto, newCommentParam));
        verify(eventClient, times(1)).existsById(1L);
        verify(userClient, never()).existsById(anyLong());
        verify(eventCommentRepository, never()).save(any(EventComment.class));
    }

    // ==================== updateComment ====================

    @Test
    @DisplayName("updateComment - должен успешно обновить комментарий и сбросить статус в WAITING")
    void updateComment_shouldUpdateComment() {
        comment.setStatus(EventCommentStatus.APPROVED);

        when(eventCommentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(eventClient.existsById(1L)).thenReturn(true);
        when(userClient.existsById(1L)).thenReturn(true);
        when(eventCommentRepository.save(any(EventComment.class))).thenReturn(comment);
        when(eventClient.findById(1L)).thenReturn(event);
        when(userClient.findUserById(1L)).thenReturn(user);

        EventCommentAuthorDto result = service.updateComment(updateDto, paramDto);

        assertNotNull(result);
        assertEquals(comment.getId(), result.getId());
        assertEquals("Updated comment", result.getContent());
        assertEquals(EventCommentStatus.WAITING, result.getStatus());
        verify(eventCommentRepository, times(1)).findById(1L);
        verify(eventClient, times(1)).existsById(1L);
        verify(userClient, times(1)).existsById(1L);
        verify(eventCommentRepository, times(1)).save(any(EventComment.class));
        verify(eventClient, times(1)).findById(1L);
        verify(userClient, times(1)).findUserById(1L);
    }

    @Test
    @DisplayName("updateComment - должен выбросить ConflictException если комментарий не принадлежит событию")
    void updateComment_shouldThrowConflictException_whenCommentNotBelongToEvent() {
        comment.setEvent(2L);

        when(eventCommentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(eventClient.existsById(1L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.updateComment(updateDto, paramDto));
        verify(eventCommentRepository, times(1)).findById(1L);
        verify(eventClient, times(1)).existsById(1L);
        verify(userClient, never()).existsById(anyLong());
        verify(eventCommentRepository, never()).save(any(EventComment.class));
    }

    @Test
    @DisplayName("updateComment - должен выбросить AccessDeniedException если пользователь не автор комментария")
    void updateComment_shouldThrowAccessDeniedException_whenUserNotAuthor() {
        comment.setAuthor(2L);

        when(eventCommentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(eventClient.existsById(1L)).thenReturn(true);
        when(userClient.existsById(1L)).thenReturn(true);

        assertThrows(AccessDeniedException.class, () -> service.updateComment(updateDto, paramDto));
        verify(eventCommentRepository, times(1)).findById(1L);
        verify(eventClient, times(1)).existsById(1L);
        verify(userClient, times(1)).existsById(1L);
        verify(eventCommentRepository, never()).save(any(EventComment.class));
    }

    // ==================== deleteComment ====================

    @Test
    @DisplayName("deleteComment - должен успешно удалить комментарий")
    void deleteComment_shouldDeleteComment() {
        when(eventCommentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(eventClient.existsById(1L)).thenReturn(true);
        when(userClient.existsById(1L)).thenReturn(true);
        doNothing().when(eventCommentRepository).delete(any(EventComment.class));

        assertDoesNotThrow(() -> service.deleteComment(paramDto));
        verify(eventCommentRepository, times(1)).findById(1L);
        verify(eventClient, times(1)).existsById(1L);
        verify(userClient, times(1)).existsById(1L);
        verify(eventCommentRepository, times(1)).delete(any(EventComment.class));
    }

    // ==================== getCommentsByEvent ====================

    @Test
    @DisplayName("getCommentsByEvent - должен вернуть одобренные комментарии события")
    void getCommentsByEvent_shouldReturnApprovedComments() {
        when(eventClient.existsById(1L)).thenReturn(true);
        Page<EventComment> page = new PageImpl<>(List.of(approvedComment));
        when(eventCommentRepository.findByEventAndStatus(eq(1L), eq(EventCommentStatus.APPROVED), any(Pageable.class)))
                .thenReturn(page);
        when(userClient.getAuthorNameBatch(List.of(1L)))
                .thenReturn(List.<Object[]>of(new Object[]{1L, "Test User"}));

        List<EventCommentUserDto> result = service.getCommentsByEvent(1L, getParamDto);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(eventClient, times(1)).existsById(1L);
        verify(eventCommentRepository, times(1))
                .findByEventAndStatus(eq(1L), eq(EventCommentStatus.APPROVED), any(Pageable.class));
        verify(userClient, times(1)).getAuthorNameBatch(anyList());
        verify(userClient, never()).findUserById(anyLong());
    }

    @Test
    @DisplayName("getCommentsByEvent - должен выбросить NotFoundException если событие не найдено")
    void getCommentsByEvent_shouldThrowNotFoundException_whenEventNotFound() {
        when(eventClient.existsById(1L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> service.getCommentsByEvent(1L, getParamDto));
        verify(eventClient, times(1)).existsById(1L);
        verify(eventCommentRepository, never())
                .findByEventAndStatus(anyLong(), any(EventCommentStatus.class), any(Pageable.class));
    }

    // ==================== getCommentsEventByUser ====================

    @Test
    @DisplayName("getCommentsEventByUser - должен вернуть одобренные комментарии пользователя по событию")
    void getCommentsEventByUser_shouldReturnApprovedComments() {
        when(eventClient.existsById(1L)).thenReturn(true);
        when(userClient.existsById(1L)).thenReturn(true);
        when(eventCommentRepository.findByEventAndAuthorAndStatusOrderByCreatedDesc(
                1L, 1L, EventCommentStatus.APPROVED))
                .thenReturn(List.of(approvedComment));
        when(eventClient.getEventTitleBatch(List.of(1L)))
                .thenReturn(List.<Object[]>of(new Object[]{1L, "Test Event"}));
        when(userClient.getAuthorNameBatch(List.of(1L)))
                .thenReturn(List.<Object[]>of(new Object[]{1L, "Test User"}));

        List<EventCommentAuthorDto> result = service.getCommentsEventByUser(paramDto);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(eventClient, times(1)).existsById(1L);
        verify(userClient, times(1)).existsById(1L);
        verify(eventCommentRepository, times(1))
                .findByEventAndAuthorAndStatusOrderByCreatedDesc(
                        1L, 1L, EventCommentStatus.APPROVED);
        verify(eventClient, times(1)).getEventTitleBatch(anyList());
        verify(userClient, times(1)).getAuthorNameBatch(anyList());
        verify(eventClient, never()).findById(anyLong());
        verify(userClient, never()).findUserById(anyLong());
    }

    @Test
    @DisplayName("getCommentsEventByUser - должен выбросить NotFoundException если событие не найдено")
    void getCommentsEventByUser_shouldThrowNotFoundException_whenEventNotFound() {
        when(eventClient.existsById(1L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> service.getCommentsEventByUser(paramDto));
        verify(eventClient, times(1)).existsById(1L);
        verify(userClient, never()).existsById(anyLong());
        verify(eventCommentRepository, never())
                .findByEventAndAuthorAndStatusOrderByCreatedDesc(anyLong(), anyLong(), any());
    }

    @Test
    @DisplayName("getCommentsEventByUser - должен выбросить NotFoundException если пользователь не найден")
    void getCommentsEventByUser_shouldThrowNotFoundException_whenUserNotFound() {
        when(eventClient.existsById(1L)).thenReturn(true);
        when(userClient.existsById(1L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> service.getCommentsEventByUser(paramDto));
        verify(eventClient, times(1)).existsById(1L);
        verify(userClient, times(1)).existsById(1L);
        verify(eventCommentRepository, never())
                .findByEventAndAuthorAndStatusOrderByCreatedDesc(anyLong(), anyLong(), any());
    }

    // ==================== approve ====================

    @Test
    @DisplayName("approve - должен одобрить комментарий")
    void approve_shouldApproveComment() {
        when(eventCommentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(eventCommentRepository.save(any(EventComment.class))).thenReturn(comment);
        when(eventClient.findById(1L)).thenReturn(event);
        when(userClient.findUserById(1L)).thenReturn(user);

        EventCommentAdminDto result = service.approve(1L);

        assertNotNull(result);
        assertEquals(EventCommentStatus.APPROVED, result.getStatus());
        verify(eventCommentRepository, times(1)).findById(1L);
        verify(eventCommentRepository, times(1)).save(any(EventComment.class));
        verify(eventClient, times(1)).findById(1L);
        verify(userClient, times(1)).findUserById(1L);
    }

    // ==================== reject ====================

    @Test
    @DisplayName("reject - должен отклонить комментарий")
    void reject_shouldRejectComment() {
        when(eventCommentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(eventCommentRepository.save(any(EventComment.class))).thenReturn(comment);
        when(eventClient.findById(1L)).thenReturn(event);
        when(userClient.findUserById(1L)).thenReturn(user);

        EventCommentAdminDto result = service.reject(1L);

        assertNotNull(result);
        assertEquals(EventCommentStatus.REJECTED, result.getStatus());
        verify(eventCommentRepository, times(1)).findById(1L);
        verify(eventCommentRepository, times(1)).save(any(EventComment.class));
        verify(eventClient, times(1)).findById(1L);
        verify(userClient, times(1)).findUserById(1L);
    }

    // ==================== getCommentById ====================

    @Test
    @DisplayName("getCommentById - должен вернуть комментарий по ID")
    void getCommentById_shouldReturnComment() {
        when(eventCommentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(eventClient.findById(1L)).thenReturn(event);
        when(userClient.findUserById(1L)).thenReturn(user);

        EventCommentAdminDto result = service.getCommentById(1L);

        assertNotNull(result);
        assertEquals(comment.getId(), result.getId());
        assertEquals(comment.getContent(), result.getContent());
        verify(eventCommentRepository, times(1)).findById(1L);
        verify(eventClient, times(1)).findById(1L);
        verify(userClient, times(1)).findUserById(1L);
    }

    // ==================== getAllComments ====================

    @Test
    @DisplayName("getAllComments - должен вернуть все комментарии при статусе ALL")
    void getAllComments_shouldReturnAllComments_whenStatusIsAll() {
        EventCommentRequestStatus state = EventCommentRequestStatus.ALL;
        Page<EventComment> page = new PageImpl<>(List.of(comment, approvedComment, rejectedComment));
        when(eventCommentRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(eventClient.getEventTitleBatch(anyList()))
                .thenReturn(List.<Object[]>of(new Object[]{1L, "Test Event"}));
        when(userClient.findShortByIds(anyList()))
                .thenReturn(List.of(user));

        List<EventCommentAdminDto> result = service.getAllComments(state, getParamDto);

        assertNotNull(result);
        assertEquals(3, result.size());
        verify(eventCommentRepository, times(1)).findAll(any(Pageable.class));
        verify(eventCommentRepository, never()).findByStatus(any(EventCommentStatus.class), any(Pageable.class));
        verify(eventClient, times(1)).getEventTitleBatch(anyList());
        verify(userClient, times(1)).findShortByIds(anyList());
        verify(eventClient, never()).findById(anyLong());
        verify(userClient, never()).findUserById(anyLong());
    }

    @Test
    @DisplayName("getAllComments - должен вернуть комментарии со статусом WAITING")
    void getAllComments_shouldReturnWaitingComments() {
        EventCommentRequestStatus state = EventCommentRequestStatus.WAITING;
        Page<EventComment> page = new PageImpl<>(List.of(comment));
        when(eventCommentRepository.findByStatus(eq(EventCommentStatus.WAITING), any(Pageable.class)))
                .thenReturn(page);
        when(eventClient.getEventTitleBatch(anyList()))
                .thenReturn(List.<Object[]>of(new Object[]{1L, "Test Event"}));
        when(userClient.findShortByIds(anyList()))
                .thenReturn(List.of(user));

        List<EventCommentAdminDto> result = service.getAllComments(state, getParamDto);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(EventCommentStatus.WAITING, result.getFirst().getStatus());
        verify(eventCommentRepository, times(1)).findByStatus(eq(EventCommentStatus.WAITING), any(Pageable.class));
        verify(eventCommentRepository, never()).findAll(any(Pageable.class));
        verify(eventClient, times(1)).getEventTitleBatch(anyList());
        verify(userClient, times(1)).findShortByIds(anyList());
        verify(eventClient, never()).findById(anyLong());
        verify(userClient, never()).findUserById(anyLong());
    }

    // ==================== ConflictException в approve/reject ====================

    @Test
    @DisplayName("approve - должен выбросить ConflictException если комментарий уже APPROVED")
    void approve_shouldThrowConflictException_whenCommentAlreadyApproved() {
        comment.setStatus(EventCommentStatus.APPROVED);
        when(eventCommentRepository.findById(1L)).thenReturn(Optional.of(comment));

        ConflictException exception = assertThrows(ConflictException.class, () -> service.approve(1L));

        assertTrue(exception.getMessage().contains("APPROVED"));
        assertTrue(exception.getMessage().contains("id=1"));
        verify(eventCommentRepository, times(1)).findById(1L);
        verify(eventCommentRepository, never()).save(any(EventComment.class));
    }

    @Test
    @DisplayName("reject - должен выбросить ConflictException если комментарий уже REJECTED")
    void reject_shouldThrowConflictException_whenCommentAlreadyRejected() {
        comment.setStatus(EventCommentStatus.REJECTED);
        when(eventCommentRepository.findById(1L)).thenReturn(Optional.of(comment));

        ConflictException exception = assertThrows(ConflictException.class, () -> service.reject(1L));

        assertTrue(exception.getMessage().contains("REJECTED"));
        assertTrue(exception.getMessage().contains("id=1"));
        verify(eventCommentRepository, times(1)).findById(1L);
        verify(eventCommentRepository, never()).save(any(EventComment.class));
    }
}