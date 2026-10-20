package ru.practicum.stat.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.stat.dto.EndpointHit;
import ru.practicum.stat.dto.ViewStats;
import ru.practicum.stat.client.exception.StatsServerUnavailable;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
public class StatClient {

    public static final String EVENT_URI = "/events";

    private final RestClient restClient;
    private final String app;
    private final DiscoveryClient discoveryClient;
    private final String statsServiceId;
    private final RetryTemplate retryTemplate;

    public StatClient(String app,
                      DiscoveryClient discoveryClient,
                      String statsServiceId,
                      RetryTemplate retryTemplate) {
        this.restClient = RestClient.create();
        this.app = app;
        this.discoveryClient = discoveryClient;
        this.statsServiceId = statsServiceId;
        this.retryTemplate = retryTemplate;
    }

    private ServiceInstance getInstance() {
        try {
            return discoveryClient
                    .getInstances(statsServiceId)
                    .getFirst();
        } catch (Exception exception) {
            throw new StatsServerUnavailable(
                    "Ошибка обнаружения адреса сервиса статистики с id: " + statsServiceId,
                    exception
            );
        }
    }

    private String makeBaseUrl() {
        ServiceInstance instance = retryTemplate.execute(cxt -> getInstance());
        return "http://" + instance.getHost() + ":" + instance.getPort();
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
            String url = makeBaseUrl() + "/hit";
            ResponseEntity<Void> response = restClient.post()
                    .uri(url)
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
            String baseUrl = makeBaseUrl();
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl + "/stats")
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