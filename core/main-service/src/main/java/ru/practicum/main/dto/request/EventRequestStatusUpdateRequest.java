package ru.practicum.main.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import ru.practicum.main.dto.enums.RequestStatus;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRequestStatusUpdateRequest {
    @NotNull
    private List<Long> requestIds;

    @NotNull
    private RequestStatus status;
}