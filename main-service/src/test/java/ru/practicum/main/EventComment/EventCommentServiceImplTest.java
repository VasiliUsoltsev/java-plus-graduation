package ru.practicum.main.EventComment;

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
import ru.practicum.main.dto.request.*;
import ru.practicum.main.dto.response.EventCommentAdminDto;
import ru.practicum.main.dto.response.EventCommentAuthorDto;
import ru.practicum.main.dto.response.EventCommentUserDto;
import ru.practicum.main.exception.model.AccessDeniedException;
import ru.practicum.main.exception.model.ConflictException;
import ru.practicum.main.exception.model.NotFoundException;
import ru.practicum.main.model.Event;
import ru.practicum.main.model.EventComment;
import ru.practicum.main.model.User;
import ru.practicum.main.model.enums.EventCommentStatus;
import ru.practicum.main.repository.EventCommentRepository;
import ru.practicum.main.repository.EventRepository;
import ru.practicum.main.repository.UserRepository;
import ru.practicum.main.service.impl.EventCommentServiceImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventCommentServiceImplTest {

    @Mock
    private EventCommentRepository eventCommentRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private EventCommentServiceImpl service;

    private User user;
    private User anotherUser;
    private Event event;
    private Event anotherEvent;
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
        user = User.builder()
                .id(1L)
                .name("Test User")
                .email("test@mail.com")
                .build();

        anotherUser = User.builder()
                .id(2L)
                .name("Another User")
                .email("another@mail.com")
                .build();

        event = Event.builder()
                .id(1L)
                .title("Test Event")
                .build();

        anotherEvent = Event.builder()
                .id(2L)
                .title("Another Event")
                .build();

        comment = EventComment.builder()
                .id(1L)
                .content("Test comment")
                .author(user)
                .event(event)
                .status(EventCommentStatus.WAITING)
                .created(LocalDateTime.now())
                .build();

        approvedComment = EventComment.builder()
                .id(2L)
                .content("Approved comment")
                .author(user)
                .event(event)
                .status(EventCommentStatus.APPROVED)
                .created(LocalDateTime.now())
                .build();

        rejectedComment = EventComment.builder()
                .id(3L)
                .content("Rejected comment")
                .author(user)
                .event(event)
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
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(eventCommentRepository.save(any(EventComment.class))).thenReturn(comment);

        EventCommentAuthorDto result = service.addComment(newCommentDto, newCommentParam);

        assertNotNull(result);
        assertEquals(comment.getId(), result.getId());
        assertEquals(comment.getContent(), result.getContent());
        assertEquals(EventCommentStatus.WAITING, result.getStatus());
        verify(eventRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).findById(1L);
        verify(eventCommentRepository, times(1)).save(any(EventComment.class));
    }

    @Test
    @DisplayName("addComment - должен выбросить NotFoundException если событие не найдено")
    void addComment_shouldThrowNotFoundException_whenEventNotFound() {
        when(eventRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.addComment(newCommentDto, newCommentParam));
        verify(eventRepository, times(1)).findById(1L);
        verify(userRepository, never()).findById(anyLong());
        verify(eventCommentRepository, never()).save(any(EventComment.class));
    }

    @Test
    @DisplayName("addComment - должен выбросить NotFoundException если пользователь не найден")
    void addComment_shouldThrowNotFoundException_whenUserNotFound() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.addComment(newCommentDto, newCommentParam));
        verify(eventRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).findById(1L);
        verify(eventCommentRepository, never()).save(any(EventComment.class));
    }

    // ==================== updateComment ====================

    @Test
    @DisplayName("updateComment - должен успешно обновить комментарий и сбросить статус в WAITING")
    void updateComment_shouldUpdateComment() {
        comment.setStatus(EventCommentStatus.APPROVED);

        when(eventCommentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(eventRepository.existsById(1L)).thenReturn(true);
        when(userRepository.existsById(1L)).thenReturn(true);
        when(eventCommentRepository.save(any(EventComment.class))).thenReturn(comment);

        EventCommentAuthorDto result = service.updateComment(updateDto, paramDto);

        assertNotNull(result);
        assertEquals(comment.getId(), result.getId());
        assertEquals("Updated comment", result.getContent());
        assertEquals(EventCommentStatus.WAITING, result.getStatus());
        verify(eventCommentRepository, times(1)).findById(1L);
        verify(eventRepository, times(1)).existsById(1L);
        verify(userRepository, times(1)).existsById(1L);
        verify(eventCommentRepository, times(1)).save(any(EventComment.class));
    }

    @Test
    @DisplayName("updateComment - должен выбросить NotFoundException если комментарий не найден")
    void updateComment_shouldThrowNotFoundException_whenCommentNotFound() {
        when(eventCommentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.updateComment(updateDto, paramDto));
        verify(eventCommentRepository, times(1)).findById(1L);
        verify(eventRepository, never()).existsById(anyLong());
        verify(userRepository, never()).existsById(anyLong());
        verify(eventCommentRepository, never()).save(any(EventComment.class));
    }

    @Test
    @DisplayName("updateComment - должен выбросить NotFoundException если событие не найдено")
    void updateComment_shouldThrowNotFoundException_whenEventNotFound() {
        when(eventCommentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(eventRepository.existsById(1L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> service.updateComment(updateDto, paramDto));
        verify(eventCommentRepository, times(1)).findById(1L);
        verify(eventRepository, times(1)).existsById(1L);
        verify(userRepository, never()).existsById(anyLong());
        verify(eventCommentRepository, never()).save(any(EventComment.class));
    }

    @Test
    @DisplayName("updateComment - должен выбросить NotFoundException если пользователь не найден")
    void updateComment_shouldThrowNotFoundException_whenUserNotFound() {
        when(eventCommentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(eventRepository.existsById(1L)).thenReturn(true);
        when(userRepository.existsById(1L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> service.updateComment(updateDto, paramDto));
        verify(eventCommentRepository, times(1)).findById(1L);
        verify(eventRepository, times(1)).existsById(1L);
        verify(userRepository, times(1)).existsById(1L);
        verify(eventCommentRepository, never()).save(any(EventComment.class));
    }

    @Test
    @DisplayName("updateComment - должен выбросить ConflictException если комментарий не принадлежит событию")
    void updateComment_shouldThrowConflictException_whenCommentNotBelongToEvent() {
        comment.setEvent(anotherEvent);

        when(eventCommentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(eventRepository.existsById(1L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.updateComment(updateDto, paramDto));
        verify(eventCommentRepository, times(1)).findById(1L);
        verify(eventRepository, times(1)).existsById(1L);
        verify(userRepository, never()).existsById(anyLong());
        verify(eventCommentRepository, never()).save(any(EventComment.class));
    }

    @Test
    @DisplayName("updateComment - должен выбросить AccessDeniedException если пользователь не автор комментария")
    void updateComment_shouldThrowAccessDeniedException_whenUserNotAuthor() {
        comment.setAuthor(anotherUser);

        when(eventCommentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(eventRepository.existsById(1L)).thenReturn(true);
        when(userRepository.existsById(1L)).thenReturn(true);

        assertThrows(AccessDeniedException.class, () -> service.updateComment(updateDto, paramDto));
        verify(eventCommentRepository, times(1)).findById(1L);
        verify(eventRepository, times(1)).existsById(1L);
        verify(userRepository, times(1)).existsById(1L);
        verify(eventCommentRepository, never()).save(any(EventComment.class));
    }

    // ==================== deleteComment ====================

    @Test
    @DisplayName("deleteComment - должен успешно удалить комментарий")
    void deleteComment_shouldDeleteComment() {
        when(eventCommentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(eventRepository.existsById(1L)).thenReturn(true);
        when(userRepository.existsById(1L)).thenReturn(true);
        doNothing().when(eventCommentRepository).delete(any(EventComment.class));

        assertDoesNotThrow(() -> service.deleteComment(paramDto));
        verify(eventCommentRepository, times(1)).findById(1L);
        verify(eventRepository, times(1)).existsById(1L);
        verify(userRepository, times(1)).existsById(1L);
        verify(eventCommentRepository, times(1)).delete(any(EventComment.class));
    }

    @Test
    @DisplayName("deleteComment - должен выбросить NotFoundException если комментарий не найден")
    void deleteComment_shouldThrowNotFoundException_whenCommentNotFound() {
        when(eventCommentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.deleteComment(paramDto));
        verify(eventCommentRepository, times(1)).findById(1L);
        verify(eventRepository, never()).existsById(anyLong());
        verify(userRepository, never()).existsById(anyLong());
        verify(eventCommentRepository, never()).delete(any(EventComment.class));
    }

    // ==================== getCommentsByEvent ====================

    @Test
    @DisplayName("getCommentsByEvent - должен вернуть одобренные комментарии события")
    void getCommentsByEvent_shouldReturnApprovedComments() {
        when(eventRepository.existsById(1L)).thenReturn(true);
        Page<EventComment> page = new PageImpl<>(List.of(approvedComment));
        when(eventCommentRepository.findByEventIdAndStatus(eq(1L), eq(EventCommentStatus.APPROVED), any(Pageable.class)))
                .thenReturn(page);

        List<EventCommentUserDto> result = service.getCommentsByEvent(1L, getParamDto);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(approvedComment.getContent(), result.getFirst().getContent());
        verify(eventRepository, times(1)).existsById(1L);
        verify(eventCommentRepository, times(1))
                .findByEventIdAndStatus(eq(1L), eq(EventCommentStatus.APPROVED), any(Pageable.class));
    }

    @Test
    @DisplayName("getCommentsByEvent - должен вернуть пустой список если нет одобренных комментариев")
    void getCommentsByEvent_shouldReturnEmptyList() {
        when(eventRepository.existsById(1L)).thenReturn(true);
        Page<EventComment> page = new PageImpl<>(List.of());
        when(eventCommentRepository.findByEventIdAndStatus(eq(1L), eq(EventCommentStatus.APPROVED), any(Pageable.class)))
                .thenReturn(page);

        List<EventCommentUserDto> result = service.getCommentsByEvent(1L, getParamDto);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(eventRepository, times(1)).existsById(1L);
        verify(eventCommentRepository, times(1))
                .findByEventIdAndStatus(eq(1L), eq(EventCommentStatus.APPROVED), any(Pageable.class));
    }

    @Test
    @DisplayName("getCommentsByEvent - должен выбросить NotFoundException если событие не найдено")
    void getCommentsByEvent_shouldThrowNotFoundException_whenEventNotFound() {
        when(eventRepository.existsById(1L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> service.getCommentsByEvent(1L, getParamDto));
        verify(eventRepository, times(1)).existsById(1L);
        verify(eventCommentRepository, never())
                .findByEventIdAndStatus(anyLong(), any(EventCommentStatus.class), any(Pageable.class));
    }

    // ==================== getCommentsEventByUser ====================

    @Test
    @DisplayName("getCommentsEventByUser - должен вернуть одобренные комментарии пользователя по событию")
    void getCommentsEventByUser_shouldReturnApprovedComments() {
        when(eventRepository.existsById(1L)).thenReturn(true);
        when(userRepository.existsById(1L)).thenReturn(true);
        when(eventCommentRepository.findByEventIdAndAuthorIdAndStatusOrderByCreatedDesc(
                1L, 1L, EventCommentStatus.APPROVED))
                .thenReturn(List.of(approvedComment));

        List<EventCommentAuthorDto> result = service.getCommentsEventByUser(paramDto);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(eventRepository, times(1)).existsById(1L);
        verify(userRepository, times(1)).existsById(1L);
        verify(eventCommentRepository, times(1))
                .findByEventIdAndAuthorIdAndStatusOrderByCreatedDesc(
                        1L, 1L, EventCommentStatus.APPROVED);
    }

    @Test
    @DisplayName("getCommentsEventByUser - должен вернуть пустой список если нет одобренных комментариев")
    void getCommentsEventByUser_shouldReturnEmptyList() {
        when(eventRepository.existsById(1L)).thenReturn(true);
        when(userRepository.existsById(1L)).thenReturn(true);
        when(eventCommentRepository.findByEventIdAndAuthorIdAndStatusOrderByCreatedDesc(
                1L, 1L, EventCommentStatus.APPROVED))
                .thenReturn(List.of());

        List<EventCommentAuthorDto> result = service.getCommentsEventByUser(paramDto);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(eventRepository, times(1)).existsById(1L);
        verify(userRepository, times(1)).existsById(1L);
        verify(eventCommentRepository, times(1))
                .findByEventIdAndAuthorIdAndStatusOrderByCreatedDesc(
                        1L, 1L, EventCommentStatus.APPROVED);
    }

    @Test
    @DisplayName("getCommentsEventByUser - должен выбросить NotFoundException если событие не найдено")
    void getCommentsEventByUser_shouldThrowNotFoundException_whenEventNotFound() {
        when(eventRepository.existsById(1L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> service.getCommentsEventByUser(paramDto));
        verify(eventRepository, times(1)).existsById(1L);
        verify(userRepository, never()).existsById(anyLong());
        verify(eventCommentRepository, never())
                .findByEventIdAndAuthorIdAndStatusOrderByCreatedDesc(anyLong(), anyLong(), any());
    }

    @Test
    @DisplayName("getCommentsEventByUser - должен выбросить NotFoundException если пользователь не найден")
    void getCommentsEventByUser_shouldThrowNotFoundException_whenUserNotFound() {
        when(eventRepository.existsById(1L)).thenReturn(true);
        when(userRepository.existsById(1L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> service.getCommentsEventByUser(paramDto));
        verify(eventRepository, times(1)).existsById(1L);
        verify(userRepository, times(1)).existsById(1L);
        verify(eventCommentRepository, never())
                .findByEventIdAndAuthorIdAndStatusOrderByCreatedDesc(anyLong(), anyLong(), any());
    }

    // ==================== approve ====================

    @Test
    @DisplayName("approve - должен одобрить комментарий")
    void approve_shouldApproveComment() {
        when(eventCommentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(eventCommentRepository.save(any(EventComment.class))).thenReturn(comment);

        EventCommentAdminDto result = service.approve(1L);

        assertNotNull(result);
        assertEquals(EventCommentStatus.APPROVED, result.getStatus());
        assertEquals(user.getId(), result.getAuthor().getId());
        verify(eventCommentRepository, times(1)).findById(1L);
        verify(eventCommentRepository, times(1)).save(any(EventComment.class));
    }

    @Test
    @DisplayName("approve - должен выбросить NotFoundException если комментарий не найден")
    void approve_shouldThrowNotFoundException_whenCommentNotFound() {
        when(eventCommentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.approve(1L));
        verify(eventCommentRepository, times(1)).findById(1L);
        verify(eventCommentRepository, never()).save(any(EventComment.class));
    }

    // ==================== reject ====================

    @Test
    @DisplayName("reject - должен отклонить комментарий")
    void reject_shouldRejectComment() {
        when(eventCommentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(eventCommentRepository.save(any(EventComment.class))).thenReturn(comment);

        EventCommentAdminDto result = service.reject(1L);

        assertNotNull(result);
        assertEquals(EventCommentStatus.REJECTED, result.getStatus());
        verify(eventCommentRepository, times(1)).findById(1L);
        verify(eventCommentRepository, times(1)).save(any(EventComment.class));
    }

    @Test
    @DisplayName("reject - должен выбросить NotFoundException если комментарий не найден")
    void reject_shouldThrowNotFoundException_whenCommentNotFound() {
        when(eventCommentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.reject(1L));
        verify(eventCommentRepository, times(1)).findById(1L);
        verify(eventCommentRepository, never()).save(any(EventComment.class));
    }

    // ==================== getCommentById ====================

    @Test
    @DisplayName("getCommentById - должен вернуть комментарий по ID")
    void getCommentById_shouldReturnComment() {
        when(eventCommentRepository.findById(1L)).thenReturn(Optional.of(comment));

        EventCommentAdminDto result = service.getCommentById(1L);

        assertNotNull(result);
        assertEquals(comment.getId(), result.getId());
        assertEquals(comment.getContent(), result.getContent());
        verify(eventCommentRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("getCommentById - должен выбросить NotFoundException если комментарий не найден")
    void getCommentById_shouldThrowNotFoundException_whenCommentNotFound() {
        when(eventCommentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getCommentById(1L));
        verify(eventCommentRepository, times(1)).findById(1L);
    }

    // ==================== getAllComments ====================

    @Test
    @DisplayName("getAllComments - должен вернуть все комментарии при статусе ALL")
    void getAllComments_shouldReturnAllComments_whenStatusIsAll() {
        EventCommentRequestStatus state = EventCommentRequestStatus.ALL;
        Page<EventComment> page = new PageImpl<>(List.of(comment, approvedComment, rejectedComment));
        when(eventCommentRepository.findAll(any(Pageable.class))).thenReturn(page);

        List<EventCommentAdminDto> result = service.getAllComments(state, getParamDto);

        assertNotNull(result);
        assertEquals(3, result.size());
        verify(eventCommentRepository, times(1)).findAll(any(Pageable.class));
        verify(eventCommentRepository, never()).findByStatus(any(EventCommentStatus.class), any(Pageable.class));
    }

    @Test
    @DisplayName("getAllComments - должен вернуть комментарии со статусом WAITING")
    void getAllComments_shouldReturnWaitingComments() {
        EventCommentRequestStatus state = EventCommentRequestStatus.WAITING;
        Page<EventComment> page = new PageImpl<>(List.of(comment));
        when(eventCommentRepository.findByStatus(eq(EventCommentStatus.WAITING), any(Pageable.class)))
                .thenReturn(page);

        List<EventCommentAdminDto> result = service.getAllComments(state, getParamDto);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(EventCommentStatus.WAITING, result.getFirst().getStatus());
        verify(eventCommentRepository, times(1)).findByStatus(eq(EventCommentStatus.WAITING), any(Pageable.class));
        verify(eventCommentRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("getAllComments - должен вернуть комментарии со статусом APPROVED")
    void getAllComments_shouldReturnApprovedComments() {
        EventCommentRequestStatus state = EventCommentRequestStatus.APPROVED;
        Page<EventComment> page = new PageImpl<>(List.of(approvedComment));
        when(eventCommentRepository.findByStatus(eq(EventCommentStatus.APPROVED), any(Pageable.class)))
                .thenReturn(page);

        List<EventCommentAdminDto> result = service.getAllComments(state, getParamDto);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(EventCommentStatus.APPROVED, result.getFirst().getStatus());
        verify(eventCommentRepository, times(1)).findByStatus(eq(EventCommentStatus.APPROVED), any(Pageable.class));
        verify(eventCommentRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("getAllComments - должен вернуть комментарии со статусом REJECTED")
    void getAllComments_shouldReturnRejectedComments() {
        EventCommentRequestStatus state = EventCommentRequestStatus.REJECTED;
        Page<EventComment> page = new PageImpl<>(List.of(rejectedComment));
        when(eventCommentRepository.findByStatus(eq(EventCommentStatus.REJECTED), any(Pageable.class)))
                .thenReturn(page);

        List<EventCommentAdminDto> result = service.getAllComments(state, getParamDto);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(EventCommentStatus.REJECTED, result.getFirst().getStatus());
        verify(eventCommentRepository, times(1)).findByStatus(eq(EventCommentStatus.REJECTED), any(Pageable.class));
        verify(eventCommentRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("getAllComments - должен вернуть пустой список если комментариев нет")
    void getAllComments_shouldReturnEmptyList() {
        EventCommentRequestStatus state = EventCommentRequestStatus.ALL;
        Page<EventComment> page = new PageImpl<>(List.of());
        when(eventCommentRepository.findAll(any(Pageable.class))).thenReturn(page);

        List<EventCommentAdminDto> result = service.getAllComments(state, getParamDto);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(eventCommentRepository, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("approve - должен выбросить ConflictException если комментарий уже APPROVED")
    void approve_shouldThrowConflictException_whenCommentAlreadyApproved() {
        comment.setStatus(EventCommentStatus.APPROVED);
        when(eventCommentRepository.findById(1L)).thenReturn(Optional.of(comment));

        ConflictException exception = assertThrows(ConflictException.class,
                () -> service.approve(1L));

        assertTrue(exception.getMessage().contains("APPROVED"));
        assertTrue(exception.getMessage().contains("id=1"));
        assertTrue(exception.getMessage().contains("Only WAITING comments can be moderated"));
        verify(eventCommentRepository, times(1)).findById(1L);
        verify(eventCommentRepository, never()).save(any(EventComment.class));
    }

    @Test
    @DisplayName("approve - должен выбросить ConflictException если комментарий REJECTED")
    void approve_shouldThrowConflictException_whenCommentRejected() {
        comment.setStatus(EventCommentStatus.REJECTED);
        when(eventCommentRepository.findById(1L)).thenReturn(Optional.of(comment));

        ConflictException exception = assertThrows(ConflictException.class,
                () -> service.approve(1L));

        assertTrue(exception.getMessage().contains("REJECTED"));
        assertTrue(exception.getMessage().contains("id=1"));
        verify(eventCommentRepository, times(1)).findById(1L);
        verify(eventCommentRepository, never()).save(any(EventComment.class));
    }

    @Test
    @DisplayName("reject - должен выбросить ConflictException если комментарий уже REJECTED")
    void reject_shouldThrowConflictException_whenCommentAlreadyRejected() {
        comment.setStatus(EventCommentStatus.REJECTED);
        when(eventCommentRepository.findById(1L)).thenReturn(Optional.of(comment));

        ConflictException exception = assertThrows(ConflictException.class,
                () -> service.reject(1L));

        assertTrue(exception.getMessage().contains("REJECTED"));
        assertTrue(exception.getMessage().contains("id=1"));
        assertTrue(exception.getMessage().contains("Only WAITING comments can be moderated"));
        verify(eventCommentRepository, times(1)).findById(1L);
        verify(eventCommentRepository, never()).save(any(EventComment.class));
    }

    @Test
    @DisplayName("reject - должен выбросить ConflictException если комментарий APPROVED")
    void reject_shouldThrowConflictException_whenCommentApproved() {
        comment.setStatus(EventCommentStatus.APPROVED);
        when(eventCommentRepository.findById(1L)).thenReturn(Optional.of(comment));

        ConflictException exception = assertThrows(ConflictException.class,
                () -> service.reject(1L));

        assertTrue(exception.getMessage().contains("APPROVED"));
        assertTrue(exception.getMessage().contains("id=1"));
        verify(eventCommentRepository, times(1)).findById(1L);
        verify(eventCommentRepository, never()).save(any(EventComment.class));
    }
}