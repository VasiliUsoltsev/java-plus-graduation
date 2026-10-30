package ru.practicum.main.client;

import jakarta.validation.constraints.Positive;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "comment-service")
public interface EventCommentClient {
    @GetMapping("/internal/countEvent/{eventId}")
    Long countByEventId(@PathVariable @Positive Long eventId);

    @GetMapping("/internal/countEvents")
    List<Object[]> countByEventIds(@RequestParam List<Long> eventIds);
}