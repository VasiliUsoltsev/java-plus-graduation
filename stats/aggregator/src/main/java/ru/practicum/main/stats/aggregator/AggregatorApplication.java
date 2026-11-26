package ru.practicum.main.stats.aggregator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AggregatorApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(AggregatorApplication.class, args);

        AggregationStarter starter = context.getBean(AggregationStarter.class);
        Thread thread = new Thread(starter::start, "aggregator-consumer");
        thread.setDaemon(false);
        thread.start();
    }
}