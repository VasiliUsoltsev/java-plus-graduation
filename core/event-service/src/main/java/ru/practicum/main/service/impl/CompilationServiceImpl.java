package ru.practicum.main.service.impl;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.client.UserClient;
import ru.practicum.main.dto.client.UserShortDto;
import ru.practicum.main.dto.request.NewCompilationDto;
import ru.practicum.main.dto.request.UpdateCompilationRequest;
import ru.practicum.main.dto.response.CompilationDto;
import ru.practicum.main.exception.model.ConflictException;
import ru.practicum.main.exception.model.NotFoundException;
import ru.practicum.main.exception.model.UserServiceUnavailableException;
import ru.practicum.main.model.Compilation;
import ru.practicum.main.model.Event;
import ru.practicum.main.repository.CompilationRepository;
import ru.practicum.main.repository.EventRepository;
import ru.practicum.main.service.CompilationService;
import ru.practicum.main.service.mapper.CompilationMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final UserClient userClient;

    @Override
    public CompilationDto createCompilation(NewCompilationDto newCompilationDto) {
        log.info("Creating new compilation with title: {}", newCompilationDto.getTitle());

        if (compilationRepository.existsByTitle(newCompilationDto.getTitle())) {
            throw new ConflictException("Compilation with title '" + newCompilationDto.getTitle() + "' already exists");
        }

        // Получаем события по ID, если они указаны
        List<Event> events = getEventsOrThrow(newCompilationDto.getEvents());

        Compilation compilation = Compilation.builder()
                .title(newCompilationDto.getTitle())
                .pinned(newCompilationDto.getPinned() != null ? newCompilationDto.getPinned() : false)
                .events(events)
                .build();

        Compilation savedCompilation = compilationRepository.save(compilation);
        log.info("Compilation created with id: {}", savedCompilation.getId());

        Map<Long, UserShortDto> initiatorBatch = getInitiatorBatch(savedCompilation.getEvents());

        return CompilationMapper.toDto(savedCompilation, initiatorBatch);
    }

    @Override
    public CompilationDto updateCompilation(Long compId, UpdateCompilationRequest request) {
        log.info("Updating compilation with id: {}", compId);

        Compilation compilation = getCompilationOrThrow(compId);

        // Обновляем только переданные поля (не null)
        if (request.getTitle() != null) {
            if (compilationRepository.existsByTitleAndIdNot(request.getTitle(), compId)) {
                throw new ConflictException("Compilation with title '" + request.getTitle() + "' already exists");
            }
            compilation.setTitle(request.getTitle());
        }

        if (request.getPinned() != null) {
            compilation.setPinned(request.getPinned());
        }

        if (request.getEvents() != null) {
            List<Event> events = getEventsOrThrow(request.getEvents());
            compilation.setEvents(events);
        }

        Compilation updatedCompilation = compilationRepository.save(compilation);
        log.info("Compilation updated with id: {}", updatedCompilation.getId());

        Map<Long, UserShortDto> initiatorBatch = getInitiatorBatch(updatedCompilation.getEvents());

        return CompilationMapper.toDto(updatedCompilation, initiatorBatch);
    }

    @Override
    public void deleteCompilation(Long compId) {
        log.info("Deleting compilation with id: {}", compId);

        if (!compilationRepository.existsById(compId)) {
            throw new NotFoundException("Compilation not found with id: " + compId);
        }

        compilationRepository.deleteById(compId);
        log.info("Compilation deleted with id: {}", compId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompilationDto> getCompilations(Boolean pinned, Pageable pageable) {
        log.info("Getting compilations with pinned: {}, page: {}, size: {}",
                pinned, pageable.getPageNumber(), pageable.getPageSize());

        Page<Compilation> compilationsPage;

        if (pinned != null) {
            compilationsPage = compilationRepository.findByPinned(pinned, pageable);
        } else {
            compilationsPage = compilationRepository.findAll(pageable);
        }

        List<Compilation> compilations = compilationsPage.getContent();

        Map<Long, UserShortDto> initiatorBatch = getInitiatorBatchForCompilations(compilations);

        return compilations.stream()
                .map(compilation -> CompilationMapper.toDto(compilation, initiatorBatch))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CompilationDto getCompilationById(Long compId) {
        log.info("Getting compilation by id: {}", compId);

        // Используем метод с EntityGraph для загрузки событий одним запросом
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation not found with id: " + compId));

        Map<Long, UserShortDto> initiatorBatch = getInitiatorBatch(compilation.getEvents());

        return CompilationMapper.toDto(compilation, initiatorBatch);
    }

    private Compilation getCompilationOrThrow(Long compId) {
        return compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation not found with id: " + compId));
    }

    private List<Event> getEventsOrThrow(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<Event> events = eventRepository.findAllById(eventIds);

        if (events.size() != eventIds.size()) {
            List<Long> foundIds = events.stream()
                    .map(Event::getId)
                    .toList();

            List<Long> notFoundIds = eventIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .toList();

            throw new NotFoundException("Events not found with ids: " + notFoundIds);
        }

        return events;
    }

    private Map<Long, UserShortDto> getInitiatorBatch(List<Event> listEvent) {
        if (listEvent == null || listEvent.isEmpty()) {
            return Map.of();
        }

        List<Long> initiatorIds = listEvent.stream()
                .map(event -> event.getInitiator())
                .toList();

        try {
            return userClient.findShortByIds(initiatorIds).stream()
                    .collect(Collectors.toMap(
                            UserShortDto::id,
                            Function.identity()
                    ));
        } catch (FeignException exception) {
            throw new UserServiceUnavailableException(exception);
        }
    }

    private Map<Long, UserShortDto> getInitiatorBatchForCompilations(List<Compilation> compilations) {
        if (compilations == null || compilations.isEmpty()) {
            return Map.of();
        }

        List<Long> initiatorIds = compilations.stream()
                .flatMap(compilation -> compilation.getEvents().stream())
                .map(Event::getInitiator)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (initiatorIds.isEmpty()) {
            return Map.of();
        }

        try {
            return userClient.findShortByIds(initiatorIds).stream()
                    .collect(Collectors.toMap(
                            UserShortDto::id,
                            Function.identity()
                    ));
        } catch (FeignException exception) {
            throw new UserServiceUnavailableException(exception);
        }
    }
}