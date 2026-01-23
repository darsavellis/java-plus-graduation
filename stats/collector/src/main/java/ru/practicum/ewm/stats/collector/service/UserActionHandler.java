package ru.practicum.ewm.stats.collector.service;


import ru.practicum.ewm.stats.grpc.UserActionProto;

public interface UserActionHandler {
    void handle(UserActionProto request);
}
