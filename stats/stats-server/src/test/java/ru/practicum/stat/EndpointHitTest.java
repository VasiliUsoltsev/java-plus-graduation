package ru.practicum.stat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.stat.dto.EndpointHit;

import java.time.LocalDateTime;
import java.time.Month;

import static org.assertj.core.api.Assertions.assertThat;

class EndpointHitTest {

    private static final LocalDateTime FIXED_DATE = LocalDateTime.of(2000, Month.JANUARY, 1, 0, 0, 0);

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void endpointHit_shouldBeValid_whenAllFieldsAreValid() {

        EndpointHit hit = EndpointHit.builder()
                .app("test-app")
                .uri("/test/1")
                .ip("192.168.1.1")
                .timestamp(FIXED_DATE)
                .build();


        var violations = validator.validate(hit);


        assertThat(violations).isEmpty();
    }

    @Test
    void endpointHit_shouldBeInvalid_whenAppIsNull() {

        EndpointHit hit = EndpointHit.builder()
                .app(null)
                .uri("/test/1")
                .ip("192.168.1.1")
                .timestamp(FIXED_DATE)
                .build();


        var violations = validator.validate(hit);


        assertThat(violations).isNotEmpty();
    }

    @Test
    void endpointHit_shouldBeInvalid_whenUriIsNull() {

        EndpointHit hit = EndpointHit.builder()
                .app("test-app")
                .uri(null)
                .ip("192.168.1.1")
                .timestamp(FIXED_DATE)
                .build();


        var violations = validator.validate(hit);


        assertThat(violations).isNotEmpty();
    }

    @Test
    void endpointHit_shouldBeInvalid_whenIpIsNull() {

        EndpointHit hit = EndpointHit.builder()
                .app("test-app")
                .uri("/test/1")
                .ip(null)
                .timestamp(FIXED_DATE)
                .build();


        var violations = validator.validate(hit);


        assertThat(violations).isNotEmpty();
    }

    @Test
    void endpointHit_shouldBeInvalid_whenTimestampIsNull() {

        EndpointHit hit = EndpointHit.builder()
                .app("test-app")
                .uri("/test/1")
                .ip("192.168.1.1")
                .timestamp(null)
                .build();


        var violations = validator.validate(hit);


        assertThat(violations).isNotEmpty();
    }

    @Test
    void endpointHit_builder_shouldCreateCorrectObject() {

        LocalDateTime timestamp = FIXED_DATE;


        EndpointHit hit = EndpointHit.builder()
                .app("test-app")
                .uri("/test/1")
                .ip("192.168.1.1")
                .timestamp(timestamp)
                .build();

        // Then
        assertThat(hit.getApp()).isEqualTo("test-app");
        assertThat(hit.getUri()).isEqualTo("/test/1");
        assertThat(hit.getIp()).isEqualTo("192.168.1.1");
        assertThat(hit.getTimestamp()).isEqualTo(timestamp);
    }
}