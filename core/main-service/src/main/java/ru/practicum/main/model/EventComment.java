package ru.practicum.main.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import ru.practicum.main.model.enums.EventCommentStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "event_comments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 5000)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id")
    private User author;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id")
    private Event event;

    @Enumerated(EnumType.STRING)
    private EventCommentStatus status;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime created;
}
