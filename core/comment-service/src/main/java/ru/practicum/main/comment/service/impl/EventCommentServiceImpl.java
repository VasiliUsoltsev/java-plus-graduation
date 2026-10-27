package ru.practicum.main.comment.service.impl;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.comment.client.EventClient;
import ru.practicum.main.comment.client.UserClient;
import ru.practicum.main.comment.dto.client.UserDto;
import ru.practicum.main.comment.dto.request.*;
import ru.practicum.main.comment.dto.response.EventCommentAdminDto;
import ru.practicum.main.comment.dto.response.EventCommentAuthorDto;
import ru.practicum.main.comment.dto.response.EventCommentUserDto;
import ru.practicum.main.comment.exception.model.*;
import ru.practicum.main.comment.model.EventComment;
import ru.practicum.main.comment.model.enums.EventCommentStatus;
import ru.practicum.main.comment.repository.EventCommentRepository;
import ru.practicum.main.comment.service.EventCommentService;
import ru.practicum.main.comment.service.mapper.EventCommentMapper;

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
    private final EventClient eventClient;
    private final UserClient userClient;

    @Override
    @Transactional
    public EventCommentAuthorDto addComment(EventCommentDto newComment, NewEventCommentParamDto param) {
        EventComment comment = EventCommentMapper.toEventComment(newComment);

        // Проверка на существование события
        Long eventId = param.getEventId();

        if (!existsEventById(eventId)) {
            throw new NotFoundException(EVENT_NOT_FOUND_EXCEPTION + eventId);
        }
        comment.setEvent(eventId);

        // Проверка на существование пользователя
        Long userId = param.getUserId();
        if (!existsUserById(userId)) {
            throw new NotFoundException(USER_NOT_FOUND_EXCEPTION + userId);
        }
        comment.setAuthor(userId);

        comment.setStatus(EventCommentStatus.WAITING);

        comment = eventCommentRepository.save(comment);

        return EventCommentMapper.toEventCommentAuthorDto(
                comment,
                getEventTitle(eventId),
                getUser(userId).name()
        );
    }

    @Override
    @Transactional
    public EventCommentAuthorDto updateComment(EventCommentDto request, EventCommentParamDto param) {
        EventComment oldComment = validateComment(param.getEventId(), param.getUserId(), param.getCommentId());

        oldComment.setContent(request.getContent());
        oldComment.setStatus(EventCommentStatus.WAITING);

        oldComment = eventCommentRepository.save(oldComment);

        return EventCommentMapper.toEventCommentAuthorDto(
                oldComment,
                getEventTitle(param.getEventId()),
                getUser(param.getUserId()).name()
        );
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

        if (!existsEventById(eventId)) {
            throw new NotFoundException(EVENT_NOT_FOUND_EXCEPTION + eventId);
        }

        // Создаем Pageable с сортировкой по дате создания (новые сверху)
        Pageable pageable = PageRequest.of(from / size, size, Sort.by("created").descending());

        // Получаем только одобренные комментарии
        Page<EventComment> page = eventCommentRepository.findByEventAndStatus(
                eventId,
                EventCommentStatus.APPROVED,
                pageable
        );

        return page.getContent().stream()
                .map(eventComment -> EventCommentMapper.toEventCommentUserDto(
                                eventComment,
                                getUser(eventComment.getAuthor()).name()
                        )
                )
                .toList();
    }

    @Override
    public List<EventCommentAuthorDto> getCommentsEventByUser(EventCommentParamDto param) {
        Long eventId = param.getEventId();
        Long userId = param.getUserId();

        if (!existsEventById(eventId)) {
            throw new NotFoundException(EVENT_NOT_FOUND_EXCEPTION + eventId);
        }

        if (!existsUserById(userId)) {
            throw new NotFoundException(USER_NOT_FOUND_EXCEPTION + userId);
        }

        return eventCommentRepository.findByEventAndAuthorAndStatusOrderByCreatedDesc(
                        eventId,
                        userId,
                        EventCommentStatus.APPROVED
                )
                .stream()
                .map(eventComment -> EventCommentMapper.toEventCommentAuthorDto(
                                eventComment,
                                getEventTitle(eventId),
                                getUser(userId).name()
                        )
                )
                .toList();
    }

    @Override
    @Transactional
    public EventCommentAdminDto approve(Long commentId) {
        EventComment comment = setStatus(commentId, EventCommentStatus.APPROVED);

        return EventCommentMapper.toEventCommentAdminDto(
                comment,
                getEventTitle(comment.getEvent()),
                getUser(comment.getAuthor())
        );
    }

    @Override
    @Transactional
    public EventCommentAdminDto reject(Long commentId) {
        EventComment comment = setStatus(commentId, EventCommentStatus.REJECTED);

        return EventCommentMapper.toEventCommentAdminDto(
                comment,
                getEventTitle(comment.getEvent()),
                getUser(comment.getAuthor())
        );
    }

    @Override
    public EventCommentAdminDto getCommentById(Long commentId) {
        EventComment comment = eventCommentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(COMMENT_NOT_FOUND_EXCEPTION + commentId));

        return EventCommentMapper.toEventCommentAdminDto(
                comment,
                getEventTitle(comment.getEvent()),
                getUser(comment.getAuthor())
        );
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
                .map(eventComment -> EventCommentMapper.toEventCommentAdminDto(
                                eventComment,
                                getEventTitle(eventComment.getEvent()),
                                getUser(eventComment.getAuthor())
                        )
                )
                .toList();
    }

    @Override
    public long countByEvent(Long eventId) {
        return eventCommentRepository.countByEvent(eventId);
    }

    @Override
    public List<Object[]> countByEventIds(List<Long> eventIds) {
        return eventCommentRepository.countByEventIds(eventIds);
    }

    // Проверка комментария
    private EventComment validateComment(Long eventId, Long userId, Long commentId) {
        EventComment oldComment = eventCommentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(COMMENT_NOT_FOUND_EXCEPTION + commentId));

        // Проверка существования события
        if (!existsEventById(eventId)) {
            throw new NotFoundException(EVENT_NOT_FOUND_EXCEPTION + eventId);
        }

        // Проверка связанности события и комментария
        Long oldEventId = oldComment.getEvent();
        if (!eventId.equals(oldEventId)) {
            throw new ConflictException("The comment does not belong to the specified event");
        }

        // Проверка существования пользователя
        if (!existsUserById(userId)) {
            throw new NotFoundException(USER_NOT_FOUND_EXCEPTION + userId);
        }

        // Проверка связанности пользователя и комментария
        Long oldUserId = oldComment.getAuthor();
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

    private Boolean existsUserById(Long userId) {
        try {
            return userClient.existsById(userId);
        } catch (FeignException exception) {
            throw new UserServiceUnavailableException(userId, exception);
        }
    }

    private Boolean existsEventById(Long eventId) {
        try {
            return eventClient.existsById(eventId);
        } catch (FeignException exception) {
            throw new EventServiceUnavailableException(eventId, exception);
        }
    }

    private UserDto getUser(Long userId) {
        try {
            return userClient.findUserById(userId);
        } catch (FeignException.NotFound exception) {
            throw new NotFoundException("User with id=" + userId + " not found");
        } catch (FeignException exception) {
            throw new UserServiceUnavailableException(userId, exception);
        }
    }

    private String getEventTitle(Long eventId) {
        try {
            return eventClient.findById(eventId).getTitle();
        } catch (FeignException.NotFound exception) {
            throw new NotFoundException("Event with id=" + eventId + " not found");
        } catch (FeignException exception) {
            throw new EventServiceUnavailableException(eventId, exception);
        }
    }
}
