package ru.practicum.main.comment.dto.request;

public enum EventCommentRequestStatus {
    ALL,       // все
    WAITING,   // ожидает модерации
    APPROVED,  // одобрен
    REJECTED;  // отказано

    public boolean isAggregate() {
        return this == ALL;
    }
}
