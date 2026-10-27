package ru.practicum.main.request.controller;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.main.request.dto.response.ParticipationRequestDto;
import ru.practicum.main.request.model.enums.ParticipationStatus;
import ru.practicum.main.request.service.ParticipationRequestService;

import java.util.Collection;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Validated
@Slf4j
@RequestMapping("/internal")
public class UserInternalController {
    private final ParticipationRequestService requestService;

    @GetMapping
    public List<ParticipationRequestDto> findAllByIdIn(@RequestParam @NotEmpty Collection<Long> ids) {
        return requestService.findAllByIdIn(ids);
    }

    @GetMapping("/{eventId}")
    public List<ParticipationRequestDto> geEventRequests(@PathVariable @Positive Long eventId) {
        return requestService.findAllByEventId(eventId);
    }

    @GetMapping("/counteventid")
    public Long countByEventAndStatus(@RequestParam @Positive Long eventId, @RequestParam ParticipationStatus status) {
        return requestService.countByEventAndStatus(eventId, status);

    }

    @GetMapping("/counteventids")
    public List<Object[]> countByEventIdsAndStatus(@RequestParam @NotEmpty List<Long> eventIds,
                                                   @RequestParam ParticipationStatus status) {
        return requestService.countByEventIdsAndStatus(eventIds, status);
    }

    @GetMapping("/reject")
    public Integer rejectRemainingPending(@RequestParam @Positive Long eventId, @RequestParam List<Long> excludeIds) {
        return requestService.rejectRemainingPending(eventId, excludeIds);
    }

    @GetMapping("/confirmrequests")
    public Integer confirmRequestsAtomically(
            @RequestParam List<Long> ids,
            @RequestParam Long eventId,
            @RequestParam int limit
    ) {
        return requestService.confirmRequestsAtomically(ids, eventId, limit);
    }
}