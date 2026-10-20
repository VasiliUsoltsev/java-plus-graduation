package ru.practicum.stat.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.MaxAttemptsRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

@AutoConfiguration
public class StatClientAutoConfiguration {

    @Bean
    public RetryTemplate statClientRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(3000L); // 3 секунды между попытками
        retryTemplate.setBackOffPolicy(backOffPolicy);

        MaxAttemptsRetryPolicy retryPolicy = new MaxAttemptsRetryPolicy();
        retryPolicy.setMaxAttempts(3); // всего 3 попытки
        retryTemplate.setRetryPolicy(retryPolicy);

        return retryTemplate;
    }

    @Bean
    public StatClient statClient(@Value("${stat.app.name:ewm-main-service}") String appName,
                                 @Value("${stat.service.id:stat-server}") String statsServiceId,
                                 DiscoveryClient discoveryClient,
                                 RetryTemplate statClientRetryTemplate) {
        return new StatClient(appName, discoveryClient, statsServiceId, statClientRetryTemplate);
    }
}