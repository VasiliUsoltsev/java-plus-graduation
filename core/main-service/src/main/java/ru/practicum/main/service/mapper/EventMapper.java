package ru.practicum.main.service.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.practicum.main.dto.LocationDto;
import ru.practicum.main.dto.request.NewEventDto;
import ru.practicum.main.dto.response.CategoryDto;
import ru.practicum.main.dto.response.EventFullDto;
import ru.practicum.main.dto.response.EventShortDto;
import ru.practicum.main.dto.response.UserShortDto;
import ru.practicum.main.model.Category;
import ru.practicum.main.model.Event;
import ru.practicum.main.model.Location;
import ru.practicum.main.model.User;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EventMapper {

    public static Event toEntity(NewEventDto dto, User initiator, Category category) {
        return Event.builder()
                .annotation(dto.getAnnotation())
                .category(category)
                .description(dto.getDescription())
                .eventDate(dto.getEventDate())
                .location(toEntity(dto.getLocation()))
                .initiator(initiator)
                .paid(dto.getPaid() != null && dto.getPaid())
                .participantLimit(dto.getParticipantLimit() != null ? dto.getParticipantLimit() : 0)
                .requestModeration(dto.getRequestModeration() == null || dto.getRequestModeration())
                .title(dto.getTitle())
                .build();
    }

    public static EventFullDto toFullDto(Event event, Long confirmedRequests, Long views, Long countEventComment) {
        if (event == null) {
            return null;
        }
        return EventFullDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(toCategoryDto(event.getCategory()))
                .confirmedRequests(confirmedRequests)
                .createdOn(event.getCreatedOn())
                .description(event.getDescription())
                .eventDate(event.getEventDate())
                .initiator(toUserShortDto(event.getInitiator()))
                .location(toLocationDto(event.getLocation()))
                .paid(event.getPaid())
                .participantLimit(event.getParticipantLimit())
                .publishedOn(event.getPublishedOn())
                .requestModeration(event.getRequestModeration())
                .state(event.getState())
                .title(event.getTitle())
                .views(views)
                .countEventComment(countEventComment)
                .build();
    }

    public static EventShortDto toShortDto(Event event, Long confirmedRequests, Long views, Long countEventComment) {
        if (event == null) {
            return null;
        }
        return EventShortDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(toCategoryDto(event.getCategory()))
                .confirmedRequests(confirmedRequests)
                .eventDate(event.getEventDate())
                .initiator(toUserShortDto(event.getInitiator()))
                .paid(event.getPaid())
                .title(event.getTitle())
                .views(views)
                .countEventComment(countEventComment)
                .build();
    }

    public static Location toEntity(LocationDto dto) {
        if (dto == null) {
            return null;
        }
        return Location.builder()
                .lat(dto.getLat())
                .lon(dto.getLon())
                .build();
    }

    private static LocationDto toLocationDto(ru.practicum.main.model.Location location) {
        if (location == null) {
            return null;
        }
        return LocationDto.builder()
                .lat(location.getLat())
                .lon(location.getLon())
                .build();
    }

    private static CategoryDto toCategoryDto(Category category) {
        if (category == null) {
            return null;
        }
        return CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .build();
    }

    private static UserShortDto toUserShortDto(User user) {
        if (user == null) {
            return null;
        }
        return UserShortDto.builder()
                .id(user.getId())
                .name(user.getName())
                .build();
    }
}
