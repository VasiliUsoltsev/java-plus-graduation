package ru.practicum.main.request.service.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.practicum.main.request.dto.response.ParticipationRequestDto;
import ru.practicum.main.request.model.ParticipationRequest;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ParticipationRequestMapper {

    public static ParticipationRequestDto toDto(ParticipationRequest request) {
        if (request == null) {
            return null;
        }
        return ParticipationRequestDto.builder()
                .id(request.getId())
                .created(request.getCreated())
                .event(request.getEvent())
                .requester(request.getRequester())
                .status(request.getStatus())
                .build();
    }
}