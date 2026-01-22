package ru.practicum.ewm.stats.collector.mapper;

import com.google.protobuf.Timestamp;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.grpc.ActionTypeProto;
import ru.practicum.ewm.stats.grpc.UserActionProto;

import java.time.Instant;

@Component
public class UserActionMapper {
    public UserActionAvro mapToUserActionAvro(UserActionProto userActionProto) {
        Timestamp timestamp = userActionProto.getTimestamp();
        return UserActionAvro.newBuilder()
            .setUserId(userActionProto.getUserId())
            .setEventId(userActionProto.getEventId())
            .setActionType(mapToActionTypeAvro(userActionProto.getActionType()))
            .setTimestamp(Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos()))
            .build();
    }

    ActionTypeAvro mapToActionTypeAvro(ActionTypeProto actionTypeProto) {
        return switch (actionTypeProto) {
            case ACTION_VIEW -> ActionTypeAvro.VIEW;
            case ACTION_REGISTER -> ActionTypeAvro.REGISTER;
            case ACTION_LIKE -> ActionTypeAvro.LIKE;
            default -> throw new IllegalStateException("Unexpected value: " + actionTypeProto);
        };
    }
}
