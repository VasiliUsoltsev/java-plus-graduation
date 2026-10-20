package ru.practicum.stat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.stat.dto.EndpointHit;
import ru.practicum.stat.dto.StatsRequest;
import ru.practicum.stat.dto.ViewStats;
import ru.practicum.stat.model.Hit;
import ru.practicum.stat.repository.StatsRepository;
import ru.practicum.stat.service.impl.StatsServiceImpl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatsServiceImplTest {

    @Mock
    private StatsRepository statsRepository;

    @InjectMocks
    private StatsServiceImpl statsService;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private LocalDateTime baseTime;

    @BeforeEach
    void setUp() {
        baseTime = LocalDateTime.parse("2026-08-27 00:00:00", formatter);
    }

    private static StatsRequest createStatsRequest(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        return StatsRequest.builder().start(start).end(end).uris(uris).unique(unique).build();
    }

    @Test
    void saveHit_shouldSaveHitSuccessfully() {

        EndpointHit hitDto = EndpointHit.builder()
                .app("test-app")
                .uri("/test/1")
                .ip("192.168.1.1")
                .timestamp(baseTime)
                .build();

        Hit expectedHit = Hit.builder()
                .app("test-app")
                .uri("/test/1")
                .ip("192.168.1.1")
                .timestamp(baseTime)
                .build();

        when(statsRepository.save(any(Hit.class))).thenReturn(expectedHit);


        statsService.saveHit(hitDto);


        verify(statsRepository, times(1)).save(any(Hit.class));
    }

    @Test
    void getStats_withUniqueAndUris_shouldCallRepositoryWithUniqueIpAndUris() {

        LocalDateTime start = baseTime.minusMinutes(1);
        LocalDateTime end = baseTime.plusMinutes(10);
        List<String> uris = List.of("/test/1", "/test/2");
        Boolean unique = true;

        StatsRequest request = createStatsRequest(start, end, uris, unique);

        List<ViewStats> expectedStats = List.of(
                new ViewStats("test-app", "/test/1", 2L),
                new ViewStats("another-app", "/test/2", 1L)
        );

        when(statsRepository.getStatsWithUniqueIpAndUris(start, end, uris))
                .thenReturn(expectedStats);

        // When
        List<ViewStats> result = statsService.getStats(request);

        // Then
        assertThat(result).isEqualTo(expectedStats);
        verify(statsRepository, times(1)).getStatsWithUniqueIpAndUris(start, end, uris);
        verify(statsRepository, never()).getStatsWithUniqueIpWithoutUris(any(), any());
    }

    @Test
    void getStats_withUniqueAndWithoutUris_shouldCallRepositoryWithUniqueIpWithoutUris() {

        LocalDateTime start = baseTime.minusMinutes(1);
        LocalDateTime end = baseTime.plusMinutes(10);
        List<String> uris = null;
        Boolean unique = true;

        StatsRequest request = createStatsRequest(start, end, uris, unique);

        List<ViewStats> expectedStats = List.of(
                new ViewStats("test-app", "/test/1", 2L),
                new ViewStats("another-app", "/test/2", 1L),
                new ViewStats("test-app", "/test/3", 1L)
        );

        when(statsRepository.getStatsWithUniqueIpWithoutUris(start, end))
                .thenReturn(expectedStats);

        // When
        List<ViewStats> result = statsService.getStats(request);

        // Then
        assertThat(result).isEqualTo(expectedStats);
        verify(statsRepository, times(1)).getStatsWithUniqueIpWithoutUris(start, end);
        verify(statsRepository, never()).getStatsWithUniqueIpAndUris(any(), any(), any());
    }

    @Test
    void getStats_withoutUniqueAndWithUris_shouldCallRepositoryWithAllAndUris() {

        LocalDateTime start = baseTime.minusMinutes(1);
        LocalDateTime end = baseTime.plusMinutes(10);
        List<String> uris = List.of("/test/1");
        Boolean unique = false;

        StatsRequest request = createStatsRequest(start, end, uris, unique);

        List<ViewStats> expectedStats = List.of(
                new ViewStats("test-app", "/test/1", 3L)
        );

        when(statsRepository.getStatsAllAndUris(start, end, uris))
                .thenReturn(expectedStats);


        List<ViewStats> result = statsService.getStats(request);


        assertThat(result).isEqualTo(expectedStats);
        verify(statsRepository, times(1)).getStatsAllAndUris(start, end, uris);
        verify(statsRepository, never()).getStatsAllWithoutUris(any(), any());
    }

    @Test
    void getStats_withoutUniqueAndWithoutUris_shouldCallRepositoryWithAllWithoutUris() {

        LocalDateTime start = baseTime.minusMinutes(1);
        LocalDateTime end = baseTime.plusMinutes(10);
        List<String> uris = null;
        Boolean unique = false;

        StatsRequest request = createStatsRequest(start, end, uris, unique);

        List<ViewStats> expectedStats = List.of(
                new ViewStats("test-app", "/test/1", 3L),
                new ViewStats("another-app", "/test/2", 1L),
                new ViewStats("test-app", "/test/3", 1L)
        );

        when(statsRepository.getStatsAllWithoutUris(start, end))
                .thenReturn(expectedStats);


        List<ViewStats> result = statsService.getStats(request);


        assertThat(result).isEqualTo(expectedStats);
        verify(statsRepository, times(1)).getStatsAllWithoutUris(start, end);
        verify(statsRepository, never()).getStatsAllAndUris(any(), any(), any());
    }
}