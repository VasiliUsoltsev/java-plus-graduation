package ru.practicum.main.dto.response;

import lombok.*;
import ru.practicum.main.model.enums.ParticipationStatus;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipationRequestDto {
    private Long id;
    private LocalDateTime created;
    private Long event;
    private Long requester;
    private ParticipationStatus status;
}