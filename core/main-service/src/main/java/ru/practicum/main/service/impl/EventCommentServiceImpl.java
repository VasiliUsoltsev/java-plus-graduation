package ru.practicum.main.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import ru.practicum.main.service.EventCommentService;
import ru.practicum.main.service.mapper.EventCommentMapper;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventCommentServiceImpl implements EventCommentService {
    private static final String EVENT_NOT_FOUND_EXCEPTION = "Event was not found with id=";
    private static final String USER_NOT_FOUND_EXCEPTION = "User was not found with id=";
    private static final String COMMENT_NOT_FOUND_EXCEPTION = "Comment was not found with id=";
    private static final String USER_ACCESS_DENIED_EXCEPTION = "You are not the author of this comment";

    private final EventCommentRepository eventCommentRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public EventCommentAuthorDto addComment(EventCommentDto newComment, NewEventCommentParamDto param) {
        EventComment comment = EventCommentMapper.toEventComment(newComment);

        // Проверка на существование события
        Long eventId = param.getEventId();
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(EVENT_NOT_FOUND_EXCEPTION + eventId));
        comment.setEvent(event);

        // Проверка на существование пользователя
        Long userId = param.getUserId();
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(USER_NOT_FOUND_EXCEPTION + userId));
        comment.setAuthor(author);

        comment.setStatus(EventCommentStatus.WAITING);

        comment = eventCommentRepository.save(comment);

        return EventCommentMapper.toEventCommentAuthorDto(comment);
    }

    @Override
    @Transactional
    public EventCommentAuthorDto updateComment(EventCommentDto request, EventCommentParamDto param) {
        EventComment oldComment = validateComment(param.getEventId(), param.getUserId(), param.getCommentId());

        oldComment.setContent(request.getContent());
        oldComment.setStatus(EventCommentStatus.WAITING);

        oldComment = eventCommentRepository.save(oldComment);

        return EventCommentMapper.toEventCommentAuthorDto(oldComment);
    }

    @Override
    @Transactional
    public void deleteComment(EventCommentParamDto param) {
        EventComment oldComment = validateComment(param.getEventId(), param.getUserId(), param.getCommentId());

        eventCommentRepository.delete(oldComment);
    }

    @Override
    public List<EventCommentUserDto> getCommentsByEvent(Long eventId, GetEventCommentParamDto param) {
        Integer from = param.getFrom();
        Integer size = param.getSize();

        if (!eventRepository.existsById(eventId)) {
            throw new NotFoundException(EVENT_NOT_FOUND_EXCEPTION + eventId);
        }

        // Создаем Pageable с сортировкой по дате создания (новые сверху)
        Pageable pageable = PageRequest.of(from / size, size, Sort.by("created").descending());

        // Получаем только одобренные комментарии
        Page<EventComment> page = eventCommentRepository.findByEventIdAndStatus(
                eventId,
                EventCommentStatus.APPROVED,
                pageable
        );

        return page.getContent().stream()
                .map(EventCommentMapper::toEventCommentUserDto)
                .toList();
    }

    @Override
    public List<EventCommentAuthorDto> getCommentsEventByUser(EventCommentParamDto param) {
        Long eventId = param.getEventId();
        Long userId = param.getUserId();

        if (!eventRepository.existsById(eventId)) {
            throw new NotFoundException(EVENT_NOT_FOUND_EXCEPTION + eventId);
        }

        if (!userRepository.existsById(userId)) {
            throw new NotFoundException(USER_NOT_FOUND_EXCEPTION + userId);
        }

        return eventCommentRepository.findByEventIdAndAuthorIdAndStatusOrderByCreatedDesc(
                        eventId,
                        userId,
                        EventCommentStatus.APPROVED
                )
                .stream()
                .map(EventCommentMapper::toEventCommentAuthorDto)
                .toList();
    }

    @Override
    @Transactional
    public EventCommentAdminDto approve(Long commentId) {
        EventComment comment = setStatus(commentId, EventCommentStatus.APPROVED);

        return EventCommentMapper.toEventCommentAdminDto(comment);
    }

    @Override
    @Transactional
    public EventCommentAdminDto reject(Long commentId) {
        EventComment comment = setStatus(commentId, EventCommentStatus.REJECTED);

        return EventCommentMapper.toEventCommentAdminDto(comment);
    }

    @Override
    public EventCommentAdminDto getCommentById(Long commentId) {
        EventComment comment = eventCommentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(COMMENT_NOT_FOUND_EXCEPTION + commentId));

        return EventCommentMapper.toEventCommentAdminDto(comment);
    }

    @Override
    public List<EventCommentAdminDto> getAllComments(EventCommentRequestStatus state, GetEventCommentParamDto param) {
        Integer from = param.getFrom();
        Integer size = param.getSize();

        // Создаем Pageable с сортировкой по дате создания (новые сверху)
        Pageable pageable = PageRequest.of(from / size, size, Sort.by("created").descending());
        Page<EventComment> page = null;

        if (state.isAggregate()) {
            page = eventCommentRepository.findAll(pageable);
        } else {
            EventCommentStatus commentStatus = EventCommentStatus.valueOf(state.toString());

            page = eventCommentRepository.findByStatus(
                    commentStatus,
                    pageable
            );
        }

        return page.getContent().stream()
                .map(EventCommentMapper::toEventCommentAdminDto)
                .toList();
    }

    // Проверка комментария
    private EventComment validateComment(Long eventId, Long userId, Long commentId) {
        EventComment oldComment = eventCommentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(COMMENT_NOT_FOUND_EXCEPTION + commentId));

        // Проверка существования события
        if (!eventRepository.existsById(eventId)) {
            throw new NotFoundException(EVENT_NOT_FOUND_EXCEPTION + eventId);
        }

        // Проверка связанности события и комментария
        Long oldEventId = oldComment.getEvent().getId();
        if (!eventId.equals(oldEventId)) {
            throw new ConflictException("The comment does not belong to the specified event");
        }

        // Проверка существования пользователя
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException(USER_NOT_FOUND_EXCEPTION + userId);
        }

        // Проверка связанности пользователя и комментария
        Long oldUserId = oldComment.getAuthor().getId();
        if (!userId.equals(oldUserId)) {
            throw new AccessDeniedException(USER_ACCESS_DENIED_EXCEPTION);
        }

        return oldComment;
    }

    private EventComment setStatus(Long commentId, EventCommentStatus status) {
        EventComment comment = eventCommentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(COMMENT_NOT_FOUND_EXCEPTION + commentId));

        if (comment.getStatus() != EventCommentStatus.WAITING) {
            throw new ConflictException(
                    String.format(
                            "Cannot change status to %s. Comment with id=%d has status %s. " +
                                    "Only WAITING comments can be moderated",
                            status, commentId, comment.getStatus()
                    )
            );
        }

        comment.setStatus(status);

        comment = eventCommentRepository.save(comment);

        return comment;
    }
}
