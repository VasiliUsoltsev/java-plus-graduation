package ru.practicum.main.stats.collector.mapper;


import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.grpc.UserActionProto;

import java.time.Instant;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CollectorMapper {
    public static UserActionAvro mapToUserActionAvro(UserActionProto userActionProto) {
        ActionTypeAvro avroActionType = switch (userActionProto.getActionType()) {
            case ACTION_VIEW -> ActionTypeAvro.VIEW;
            case ACTION_REGISTER -> ActionTypeAvro.REGISTER;
            case ACTION_LIKE -> ActionTypeAvro.LIKE;
            case UNRECOGNIZED -> throw new IllegalArgumentException("Неопознанное действие: "
                    + userActionProto.getActionType());
        };

        Instant instant = Instant.ofEpochSecond(
               userActionProto.getTimestamp().getSeconds(),
                userActionProto.getTimestamp().getNanos()
        );
        long timestampMs = instant.toEpochMilli();

        return UserActionAvro.newBuilder()
                .setUserId(userActionProto.getUserId())
                .setEventId(userActionProto.getEventId())
                .setActionType(avroActionType)
                .setTimestamp(Instant.ofEpochMilli(timestampMs))
                .build();
    }
}
