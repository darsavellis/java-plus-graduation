package ru.practicum.ewm.stats.analyzer.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.analyzer.dal.model.UserAction;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Component
public class UserActionMapper {
    final static double VIEW_WEIGHT = 0.4;
    final static double REGISTER_WEIGHT = 0.8;
    final static double LIKE_WEIGHT = 1.0;

    public UserAction mapToUserAction(UserActionAvro userActionAvro) {
        return UserAction.builder()
            .userId(userActionAvro.getUserId())
            .eventId(userActionAvro.getEventId())
            .weight(getActionWeight(userActionAvro.getActionType()))
            .timestamp(userActionAvro.getTimestamp())
            .build();
    }

    public double getActionWeight(ActionTypeAvro actionTypeAvro) {
        return switch (actionTypeAvro) {
            case VIEW -> VIEW_WEIGHT;
            case REGISTER -> REGISTER_WEIGHT;
            case LIKE -> LIKE_WEIGHT;
        };
    }
}
