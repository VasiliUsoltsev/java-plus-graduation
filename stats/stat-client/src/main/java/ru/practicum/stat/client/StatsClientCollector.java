package ru.practicum.stat.client;

import com.google.protobuf.Timestamp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.grpc.ActionTypeProto;
import ru.practicum.ewm.stats.grpc.UserActionControllerGrpc;
import ru.practicum.ewm.stats.grpc.UserActionProto;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatsClientCollector {
    @GrpcClient("collector")
    private UserActionControllerGrpc.UserActionControllerBlockingStub collectorStub;

    public void send(Long userId, Long eventId, ActionTypeProto actionType) {
        Instant now = Instant.now();
        Timestamp timestampProto = Timestamp.newBuilder()
                .setSeconds(now.getEpochSecond())
                .setNanos(now.getNano())
                .build();

        UserActionProto actionProto = UserActionProto.newBuilder()
                .setEventId(eventId)
                .setUserId(userId)
                .setTimestamp(timestampProto)
                .setActionType(actionType)
                .build();
        try {
            collectorStub.collectUserAction(actionProto);

            log.info("Пользовательское действие успешно отправлено: userId={}, eventId={}, action={}",
                    userId, eventId, actionType);
        } catch (Exception e) {
            log.error("Ошибка при отправке пользовательского действия в collector-servise: {}", e.getMessage());
        }
    }
}
