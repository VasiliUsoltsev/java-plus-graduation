package ru.practicum.stat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.stat.dto.ViewStats;
import ru.practicum.stat.model.Hit;
import ru.practicum.stat.repository.StatsRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class StatsRepositoryTest {

    @Autowired
    private StatsRepository statsRepository;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private LocalDateTime baseTime;

    @BeforeEach
    void setUp() {
        baseTime = LocalDateTime.parse("2026-08-27 00:00:00", formatter);

        // Очистка базы перед каждым тестом
        statsRepository.deleteAll();
    }

    private void createTestData() {
        // Создаем тестовые данные
        Hit hit1 = Hit.builder()
                .app("test-app")
                .uri("/test/1")
                .ip("192.168.1.1")
                .timestamp(baseTime)
                .build();

        Hit hit2 = Hit.builder()
                .app("test-app")
                .uri("/test/1")
                .ip("192.168.1.2")
                .timestamp(baseTime.plusMinutes(1))
                .build();

        Hit hit3 = Hit.builder()
                .app("test-app")
                .uri("/test/1")
                .ip("192.168.1.1") // Тот же IP, что и у hit1
                .timestamp(baseTime.plusMinutes(2))
                .build();

        Hit hit4 = Hit.builder()
                .app("another-app")
                .uri("/test/2")
                .ip("192.168.1.3")
                .timestamp(baseTime.plusMinutes(3))
                .build();

        Hit hit5 = Hit.builder()
                .app("test-app")
                .uri("/test/3")
                .ip("192.168.1.4")
                .timestamp(baseTime.plusMinutes(4))
                .build();

        statsRepository.saveAll(List.of(hit1, hit2, hit3, hit4, hit5));
    }

    @Test
    void getStatsWithUniqueIpAndUris_shouldReturnUniqueHitsForSpecificUris() {

        createTestData();
        LocalDateTime start = baseTime.minusMinutes(1);
        LocalDateTime end = baseTime.plusMinutes(10);
        List<String> uris = List.of("/test/1", "/test/2");


        List<ViewStats> result = statsRepository.getStatsWithUniqueIpAndUris(start, end, uris);


        assertThat(result).hasSize(2);

        // Проверяем статистику для /test/1 - 2 уникальных IP (192.168.1.1 и 192.168.1.2)
        ViewStats stats1 = result.stream()
                .filter(s -> s.getUri().equals("/test/1"))
                .findFirst()
                .orElseThrow();
        assertThat(stats1.getApp()).isEqualTo("test-app");
        assertThat(stats1.getHits()).isEqualTo(2L);

        // Проверяем статистику для /test/2 - 1 уникальный IP
        ViewStats stats2 = result.stream()
                .filter(s -> s.getUri().equals("/test/2"))
                .findFirst()
                .orElseThrow();
        assertThat(stats2.getApp()).isEqualTo("another-app");
        assertThat(stats2.getHits()).isEqualTo(1L);
    }

    @Test
    void getStatsWithUniqueIpWithoutUris_shouldReturnUniqueHitsForAllUris() {

        createTestData();
        LocalDateTime start = baseTime.minusMinutes(1);
        LocalDateTime end = baseTime.plusMinutes(10);


        List<ViewStats> result = statsRepository.getStatsWithUniqueIpWithoutUris(start, end);


        assertThat(result).hasSize(3);

        // Проверяем, что результаты отсортированы по убыванию количества хитов
        assertThat(result.get(0).getHits()).isGreaterThanOrEqualTo(result.get(1).getHits());
        assertThat(result.get(1).getHits()).isGreaterThanOrEqualTo(result.get(2).getHits());


        ViewStats statsForTest1 = result.stream()
                .filter(s -> s.getUri().equals("/test/1"))
                .findFirst()
                .orElseThrow();
        assertThat(statsForTest1.getHits()).isEqualTo(2L);
    }

    @Test
    void getStatsAllAndUris_shouldReturnAllHitsForSpecificUris() {

        createTestData();
        LocalDateTime start = baseTime.minusMinutes(1);
        LocalDateTime end = baseTime.plusMinutes(10);
        List<String> uris = List.of("/test/1", "/test/2");


        List<ViewStats> result = statsRepository.getStatsAllAndUris(start, end, uris);


        assertThat(result).hasSize(2);

        // Проверяем статистику для /test/1 - 3 хита (не уникальных IP)
        ViewStats stats1 = result.stream()
                .filter(s -> s.getUri().equals("/test/1"))
                .findFirst()
                .orElseThrow();
        assertThat(stats1.getApp()).isEqualTo("test-app");
        assertThat(stats1.getHits()).isEqualTo(3L);
    }

    @Test
    void getStatsAllWithoutUris_shouldReturnAllHitsForAllUris() {
        // Given
        createTestData();
        LocalDateTime start = baseTime.minusMinutes(1);
        LocalDateTime end = baseTime.plusMinutes(10);

        // When
        List<ViewStats> result = statsRepository.getStatsAllWithoutUris(start, end);

        // Then
        assertThat(result).hasSize(3);

        // Проверяем суммарное количество хитов
        long totalHits = result.stream().mapToLong(ViewStats::getHits).sum();
        assertThat(totalHits).isEqualTo(5L);
    }

    @Test
    void getStats_shouldReturnEmptyList_whenNoDataInTimeRange() {
        // Given
        createTestData();
        LocalDateTime start = baseTime.plusMinutes(10);
        LocalDateTime end = baseTime.plusMinutes(20);
        List<String> uris = List.of("/test/1");

        // When
        List<ViewStats> result = statsRepository.getStatsWithUniqueIpAndUris(start, end, uris);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void getStats_shouldReturnEmptyList_whenUrisNotFound() {
        // Given
        createTestData();
        LocalDateTime start = baseTime.minusMinutes(1);
        LocalDateTime end = baseTime.plusMinutes(10);
        List<String> uris = List.of("/nonexistent");

        // When
        List<ViewStats> result = statsRepository.getStatsWithUniqueIpAndUris(start, end, uris);

        // Then
        assertThat(result).isEmpty();
    }
}