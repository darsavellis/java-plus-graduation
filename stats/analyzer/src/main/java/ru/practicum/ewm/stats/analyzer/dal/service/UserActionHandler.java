package ru.practicum.ewm.stats.analyzer.dal.service;

import ru.practicum.ewm.stats.avro.UserActionAvro;

public interface UserActionHandler {
    void handle(UserActionAvro userActionAvro);
}
