package ru.practicum.main.service.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.practicum.main.dto.response.ParticipationRequestDto;
import ru.practicum.main.model.ParticipationRequest;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ParticipationRequestMapper {

    public static ParticipationRequestDto toDto(ParticipationRequest request) {
        if (request == null) {
            return null;
        }
        return ParticipationRequestDto.builder()
                .id(request.getId())
                .created(request.getCreated())
                .event(request.getEvent() != null ? request.getEvent().getId() : null)
                .requester(request.getRequester() != null ? request.getRequester().getId() : null)
                .status(request.getStatus())
                .build();
    }
}