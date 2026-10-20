package ru.practicum.stat.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.stat.dto.EndpointHit;
import ru.practicum.stat.dto.ViewStats;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
public class StatClient {

    public static final String EVENT_URI = "/events";

    private final RestClient restClient;
    private final String app;

    public StatClient(String baseUrl, String app) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.app = app;
    }

    public boolean saveHit(String ip, String uri) {
        return saveHit(EndpointHit.builder()
                .app(app)
                .uri(uri)
                .ip(ip)
                .timestamp(LocalDateTime.now())
                .build());
    }

    public boolean saveHit(EndpointHit hit) {
        try {
            ResponseEntity<Void> response = restClient.post()
                    .uri("/hit")
                    .body(hit)
                    .retrieve()
                    .toBodilessEntity();

            if (response.getStatusCode().equals(HttpStatus.CREATED)) {
                return true;
            } else {
                log.warn("Просмотр не учтен в статистике: app={}, status={}",
                        hit.getApp(),
                        response.getStatusCode());
                return false;
            }
        } catch (Exception e) {
            log.error("Ошибка при сохранении данных об просмотре: app={}, uri={}, error={}",
                    hit.getApp(),
                    hit.getUri(),
                    e.getMessage());
            return false;
        }
    }

    public List<ViewStats> getStats(String start, String end, List<String> uris, Boolean unique) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/stats")
                    .queryParam("start", start)
                    .queryParam("end", end)
                    .queryParam("unique", unique != null && unique);

            if (uris != null && !uris.isEmpty()) {
                for (String uri : uris) {
                    builder.queryParam("uris", uri);
                }
            }

            String url = builder.build().toUriString();

            return restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<ViewStats>>() {
                    });
        } catch (Exception e) {
            log.error("Ошибка при получении статистики: {}", e.getMessage());
            return List.of();
        }

    }
}