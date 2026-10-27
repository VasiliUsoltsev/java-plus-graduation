package ru.practicum.main.comment.client;

import jakarta.validation.constraints.Positive;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.main.comment.dto.client.EventDto;

@FeignClient(name = "event-service")
public interface EventClient {
    @GetMapping("/internal/events/{eventId}")
    EventDto findById(@PathVariable @Positive Long eventId);

    @GetMapping("/internal/events/{eventId}/existsevent")
    Boolean existsById(@PathVariable @Positive Long eventId);
}
