package ru.practicum.main.compilations;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import ru.practicum.main.client.UserClient;
import ru.practicum.main.dto.client.UserShortDto;
import ru.practicum.main.dto.request.NewCompilationDto;
import ru.practicum.main.dto.request.UpdateCompilationRequest;
import ru.practicum.main.dto.response.CompilationDto;
import ru.practicum.main.exception.model.ConflictException;
import ru.practicum.main.exception.model.NotFoundException;
import ru.practicum.main.model.Compilation;
import ru.practicum.main.model.Event;
import ru.practicum.main.repository.CompilationRepository;
import ru.practicum.main.repository.EventRepository;
import ru.practicum.main.service.impl.CompilationServiceImpl;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompilationServiceImplTest {

    @Mock
    private CompilationRepository compilationRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserClient userClient;

    @InjectMocks
    private CompilationServiceImpl compilationService;

    private NewCompilationDto newCompilationDto;
    private UpdateCompilationRequest updateRequest;
    private Compilation compilation;
    private Event event;
    private List<Event> events;
    private UserShortDto userShortDto;

    @BeforeEach
    void setUp() {
        event = Event.builder()
                .id(1L)
                .title("Test Event")
                .annotation("Test Annotation")
                .paid(false)
                .initiator(1L)
                .build();

        events = List.of(event);

        userShortDto = UserShortDto.builder()
                .id(1L)
                .name("John Doe")
                .build();

        newCompilationDto = NewCompilationDto.builder()
                .title("Test Compilation")
                .pinned(true)
                .events(List.of(1L))
                .build();

        updateRequest = UpdateCompilationRequest.builder()
                .title("Updated Compilation")
                .pinned(false)
                .events(List.of(1L))
                .build();

        compilation = Compilation.builder()
                .id(1L)
                .title("Test Compilation")
                .pinned(true)
                .events(events)
                .build();
    }

    @Test
    void createCompilation_ShouldReturnCompilationDto() {
        when(compilationRepository.existsByTitle("Test Compilation")).thenReturn(false);
        when(eventRepository.findAllById(any(List.class))).thenReturn(events);
        when(compilationRepository.save(any(Compilation.class))).thenReturn(compilation);
        when(userClient.findShortByIds(anyList())).thenReturn(List.of(userShortDto));

        CompilationDto result = compilationService.createCompilation(newCompilationDto);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Test Compilation");
        assertThat(result.getPinned()).isTrue();
        assertThat(result.getEvents()).hasSize(1);

        verify(compilationRepository).existsByTitle("Test Compilation");
        verify(eventRepository).findAllById(any(List.class));
        verify(compilationRepository).save(any(Compilation.class));
        verify(userClient).findShortByIds(anyList());
    }

    @Test
    void createCompilation_WithNullEvents_ShouldCreateEmptyCompilation() {
        NewCompilationDto dtoWithoutEvents = NewCompilationDto.builder()
                .title("Empty Compilation")
                .pinned(false)
                .events(null)
                .build();

        Compilation emptyCompilation = Compilation.builder()
                .id(2L)
                .title("Empty Compilation")
                .pinned(false)
                .events(List.of())
                .build();

        when(compilationRepository.existsByTitle("Empty Compilation")).thenReturn(false);
        when(compilationRepository.save(any(Compilation.class))).thenReturn(emptyCompilation);

        CompilationDto result = compilationService.createCompilation(dtoWithoutEvents);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Empty Compilation");
        assertThat(result.getEvents()).isEmpty();

        verify(compilationRepository).existsByTitle("Empty Compilation");
        verify(eventRepository, never()).findAllById(any());
        verify(compilationRepository).save(any(Compilation.class));
        verify(userClient, never()).findShortByIds(anyList());
    }

    @Test
    void createCompilation_WithNonExistentEvents_ShouldThrowNotFoundException() {
        List<Long> eventIds = List.of(1L, 2L);
        NewCompilationDto dtoWithInvalidEvents = NewCompilationDto.builder()
                .title("Invalid Events")
                .pinned(true)
                .events(eventIds)
                .build();

        when(compilationRepository.existsByTitle("Invalid Events")).thenReturn(false);
        when(eventRepository.findAllById(eventIds)).thenReturn(List.of(event));

        assertThatThrownBy(() -> compilationService.createCompilation(dtoWithInvalidEvents))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Events not found with ids: [2]");

        verify(compilationRepository).existsByTitle("Invalid Events");
        verify(eventRepository).findAllById(eventIds);
        verify(compilationRepository, never()).save(any(Compilation.class));
    }

    @Test
    void createCompilation_WithDuplicateTitle_ShouldThrowConflictException() {
        when(compilationRepository.existsByTitle("Test Compilation")).thenReturn(true);

        assertThatThrownBy(() -> compilationService.createCompilation(newCompilationDto))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");

        verify(compilationRepository).existsByTitle("Test Compilation");
        verify(eventRepository, never()).findAllById(any());
        verify(compilationRepository, never()).save(any(Compilation.class));
    }

    @Test
    void updateCompilation_ShouldUpdateAllFields() {
        Long compId = 1L;

        Compilation updatedCompilation = Compilation.builder()
                .id(1L)
                .title("Updated Compilation")
                .pinned(false)
                .events(events)
                .build();

        when(compilationRepository.findById(compId)).thenReturn(Optional.of(compilation));
        when(compilationRepository.existsByTitleAndIdNot("Updated Compilation", compId)).thenReturn(false);
        when(eventRepository.findAllById(any(List.class))).thenReturn(events);
        when(compilationRepository.save(any(Compilation.class))).thenReturn(updatedCompilation);
        when(userClient.findShortByIds(anyList())).thenReturn(List.of(userShortDto));

        CompilationDto result = compilationService.updateCompilation(compId, updateRequest);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Updated Compilation");
        assertThat(result.getPinned()).isFalse();

        verify(compilationRepository).findById(compId);
        verify(compilationRepository).existsByTitleAndIdNot("Updated Compilation", compId);
        verify(eventRepository).findAllById(any(List.class));
        verify(compilationRepository).save(any(Compilation.class));
        verify(userClient).findShortByIds(anyList());
    }

    @Test
    void updateCompilation_ShouldUpdateOnlyTitle() {
        Long compId = 1L;
        UpdateCompilationRequest requestWithOnlyTitle = UpdateCompilationRequest.builder()
                .title("Only Title Updated")
                .build();

        Compilation compilationWithUpdatedTitle = Compilation.builder()
                .id(1L)
                .title("Only Title Updated")
                .pinned(true)
                .events(events)
                .build();

        when(compilationRepository.findById(compId)).thenReturn(Optional.of(compilation));
        when(compilationRepository.existsByTitleAndIdNot("Only Title Updated", compId)).thenReturn(false);
        when(compilationRepository.save(any(Compilation.class))).thenReturn(compilationWithUpdatedTitle);
        when(userClient.findShortByIds(anyList())).thenReturn(List.of(userShortDto));

        CompilationDto result = compilationService.updateCompilation(compId, requestWithOnlyTitle);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Only Title Updated");
        assertThat(result.getPinned()).isTrue();

        verify(compilationRepository).findById(compId);
        verify(compilationRepository).existsByTitleAndIdNot("Only Title Updated", compId);
        verify(eventRepository, never()).findAllById(any());
        verify(compilationRepository).save(any(Compilation.class));
        verify(userClient).findShortByIds(anyList());
    }

    @Test
    void updateCompilation_WithDuplicateTitle_ShouldThrowConflictException() {
        Long compId = 1L;

        when(compilationRepository.findById(compId)).thenReturn(Optional.of(compilation));
        when(compilationRepository.existsByTitleAndIdNot("Updated Compilation", compId)).thenReturn(true);

        assertThatThrownBy(() -> compilationService.updateCompilation(compId, updateRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");

        verify(compilationRepository).findById(compId);
        verify(compilationRepository).existsByTitleAndIdNot("Updated Compilation", compId);
        verify(compilationRepository, never()).save(any(Compilation.class));
    }

    @Test
    void updateCompilation_WithNonExistentCompilation_ShouldThrowNotFoundException() {
        Long compId = 999L;
        when(compilationRepository.findById(compId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> compilationService.updateCompilation(compId, updateRequest))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Compilation not found with id: " + compId);

        verify(compilationRepository).findById(compId);
        verify(compilationRepository, never()).save(any(Compilation.class));
    }

    @Test
    void deleteCompilation_ShouldDeleteExistingCompilation() {
        Long compId = 1L;
        when(compilationRepository.existsById(compId)).thenReturn(true);
        doNothing().when(compilationRepository).deleteById(compId);

        compilationService.deleteCompilation(compId);

        verify(compilationRepository).existsById(compId);
        verify(compilationRepository).deleteById(compId);
    }

    @Test
    void deleteCompilation_WithNonExistentCompilation_ShouldThrowNotFoundException() {
        Long compId = 999L;
        when(compilationRepository.existsById(compId)).thenReturn(false);

        assertThatThrownBy(() -> compilationService.deleteCompilation(compId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Compilation not found with id: " + compId);

        verify(compilationRepository).existsById(compId);
        verify(compilationRepository, never()).deleteById(any());
    }

    @Test
    void getCompilations_WithoutPinnedFilter_ShouldReturnAll() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Compilation> page = new PageImpl<>(List.of(compilation));

        when(compilationRepository.findAll(pageable)).thenReturn(page);
        when(userClient.findShortByIds(anyList())).thenReturn(List.of(userShortDto));

        List<CompilationDto> result = compilationService.getCompilations(null, pageable);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);

        verify(compilationRepository).findAll(pageable);
        verify(compilationRepository, never()).findByPinned(any(), any());
        verify(userClient).findShortByIds(anyList());
    }

    @Test
    void getCompilations_WithPinnedFilter_ShouldReturnFiltered() {
        Boolean pinned = true;
        Pageable pageable = PageRequest.of(0, 10);
        Page<Compilation> page = new PageImpl<>(List.of(compilation));

        when(compilationRepository.findByPinned(pinned, pageable)).thenReturn(page);
        when(userClient.findShortByIds(anyList())).thenReturn(List.of(userShortDto));

        List<CompilationDto> result = compilationService.getCompilations(pinned, pageable);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPinned()).isTrue();

        verify(compilationRepository).findByPinned(pinned, pageable);
        verify(compilationRepository, never()).findAll(any(Pageable.class));
        verify(userClient).findShortByIds(anyList());
    }

    @Test
    void getCompilationById_ShouldReturnCompilation() {
        Long compId = 1L;
        when(compilationRepository.findById(compId)).thenReturn(Optional.of(compilation));
        when(userClient.findShortByIds(anyList())).thenReturn(List.of(userShortDto));

        CompilationDto result = compilationService.getCompilationById(compId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Test Compilation");

        verify(compilationRepository).findById(compId);
        verify(userClient).findShortByIds(anyList());
    }

    @Test
    void getCompilationById_WithNonExistentId_ShouldThrowNotFoundException() {
        Long compId = 999L;
        when(compilationRepository.findById(compId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> compilationService.getCompilationById(compId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Compilation not found with id: " + compId);

        verify(compilationRepository).findById(compId);
    }
}