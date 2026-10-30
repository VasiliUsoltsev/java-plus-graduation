package ru.practicum.main.request.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;
import ru.practicum.main.request.model.enums.ParticipationStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "participation_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParticipationRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(nullable = false, insertable = false, updatable = false)
    @Generated(event = {EventType.INSERT})
    private LocalDateTime created;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParticipationStatus status;

    @Column(name = "event_id")
    private Long event;

    @Column(name = "requester_id")
    private Long requester;
}