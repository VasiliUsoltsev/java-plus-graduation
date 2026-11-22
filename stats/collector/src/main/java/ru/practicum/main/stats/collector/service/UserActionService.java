package ru.practicum.main.stats.collector.service;

import ru.practicum.ewm.stats.grpc.UserActionProto;

public interface UserActionService {
    void sendUserAction(UserActionProto action);
}
