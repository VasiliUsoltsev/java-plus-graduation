package ru.practicum.main.client;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.main.dto.client.ParticipationRequestDto;
import ru.practicum.main.dto.client.ParticipationStatus;

import java.util.Collection;
import java.util.List;

@FeignClient(name = "request-service")
public interface ParticipationRequestClient {
    @GetMapping("/internal/{eventId}")
    List<ParticipationRequestDto> findAllByEventId(@PathVariable @Positive Long eventId);

    @GetMapping("/internal")
    List<ParticipationRequestDto> findAllByIdIn(@RequestParam @NotEmpty Collection<Long> ids);

    @GetMapping("/internal/counteventid")
    Long countByEventAndStatus(@RequestParam @Positive Long eventId, @RequestParam ParticipationStatus status);

    @GetMapping("/internal/counteventids")
    List<Object[]> countByEventIdsAndStatus(@RequestParam @NotEmpty List<Long> eventIds,
                                            @RequestParam ParticipationStatus status);

    @GetMapping("/internal/reject")
    Integer rejectRemainingPending(@RequestParam @Positive Long eventId, @RequestParam List<Long> excludeIds);

    @GetMapping("/internal/confirmrequests")
    Integer confirmRequestsAtomically(
            @RequestParam List<Long> ids,
            @RequestParam Long eventId,
            @RequestParam int limit
    );
}

