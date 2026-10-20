package ru.practicum.stat.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class StatClientAutoConfiguration {

    @Bean
    public StatClient statClient(@Value("${stat.server.url:http://localhost:9090}") String baseUrl,
                                 @Value("${stat.app.name:ewm-main-service}") String appName) {
        return new StatClient(baseUrl, appName);
    }
}